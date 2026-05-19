package com.example.mentalcompanion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.mentalcompanion.common.JsonUtils;
import com.example.mentalcompanion.domain.entity.UserMemory;
import com.example.mentalcompanion.domain.enums.IntentType;
import com.example.mentalcompanion.domain.enums.RiskLevel;
import com.example.mentalcompanion.dto.MemoryCandidate;
import com.example.mentalcompanion.llm.LlmClient;
import com.example.mentalcompanion.mapper.UserMemoryMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class LongTermMemoryService {

    private static final int MAX_CONTEXT_ITEMS = 8;
    private static final int MAX_EXTRACTED_ITEMS = 5;

    private final UserMemoryMapper userMemoryMapper;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LongTermMemoryService(UserMemoryMapper userMemoryMapper, LlmClient llmClient, ObjectMapper objectMapper) {
        this.userMemoryMapper = userMemoryMapper;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public String context(Long userId) {
        List<UserMemory> memories = userMemoryMapper.selectList(new LambdaQueryWrapper<UserMemory>()
                .eq(UserMemory::getUserId, userId)
                .orderByDesc(UserMemory::getImportance)
                .orderByDesc(UserMemory::getUpdateTime)
                .last("LIMIT " + MAX_CONTEXT_ITEMS));
        if (memories.isEmpty()) {
            return "暂无长期记忆。";
        }
        LocalDateTime now = LocalDateTime.now();
        StringBuilder builder = new StringBuilder();
        for (UserMemory memory : memories) {
            builder.append("- [").append(memory.getMemoryType()).append("] ")
                    .append(memory.getContent()).append('\n');
            memory.setLastUsedTime(now);
            userMemoryMapper.updateById(memory);
        }
        return builder.toString();
    }

    public void extractAndSave(
            Long userId,
            Long sessionId,
            String userMessage,
            String aiReply,
            IntentType intent,
            RiskLevel riskLevel
    ) {
        try {
            List<MemoryCandidate> candidates = extractWithLlm(userMessage, aiReply, intent, riskLevel);
            if (candidates.isEmpty()) {
                candidates = fallbackCandidates(userMessage, intent, riskLevel);
            }
            candidates.stream()
                    .filter(candidate -> candidate.getContent() != null && !candidate.getContent().isBlank())
                    .limit(MAX_EXTRACTED_ITEMS)
                    .forEach(candidate -> upsert(userId, sessionId, candidate));
        } catch (RuntimeException ignored) {
            fallbackCandidates(userMessage, intent, riskLevel).forEach(candidate -> upsert(userId, sessionId, candidate));
        }
    }

    private List<MemoryCandidate> extractWithLlm(String userMessage, String aiReply, IntentType intent, RiskLevel riskLevel) {
        try {
            String systemPrompt = """
                    你是心理陪伴助手的长期记忆抽取模块。
                    只输出 JSON 数组，不要输出任何多余文字。
                    只记录后续陪伴真正有帮助的稳定信息，例如长期压力源、持续困扰、偏好的支持方式、需要后续关注的风险点。
                    不要记录医疗诊断结论，不要记录自伤或伤害他人的方法细节，不要记录身份证号、手机号、住址等高度敏感隐私。
                    如果没有值得长期保存的信息，输出 []。
                    JSON 数组元素格式：
                    {
                      "memoryType": "concern",
                      "content": "用户最近持续受工作压力和睡眠问题困扰",
                      "importance": 4
                    }
                    importance 只能是 1 到 5。
                    """;
            String userPrompt = """
                    用户输入：
                    %s

                    助手回复：
                    %s

                    当前意图：%s
                    当前风险等级：%s
                    """.formatted(userMessage, aiReply, intent, riskLevel);
            String raw = llmClient.chatJson(systemPrompt, userPrompt);
            JsonNode json = objectMapper.readTree(JsonUtils.extractJsonArray(raw));
            List<MemoryCandidate> candidates = new ArrayList<>();
            if (!json.isArray()) {
                return candidates;
            }
            for (JsonNode item : json) {
                MemoryCandidate candidate = new MemoryCandidate();
                candidate.setMemoryType(item.path("memoryType").asText("note"));
                candidate.setContent(item.path("content").asText(""));
                int importance = item.path("importance").asInt(3);
                candidate.setImportance(Math.max(1, Math.min(5, importance)));
                candidates.add(candidate);
            }
            return candidates;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<MemoryCandidate> fallbackCandidates(String userMessage, IntentType intent, RiskLevel riskLevel) {
        List<MemoryCandidate> candidates = new ArrayList<>();
        if (intent == IntentType.CONSULT || riskLevel == RiskLevel.MEDIUM) {
            candidates.add(candidate("concern", "用户曾表达情绪困扰或压力体验：" + truncate(userMessage), 3));
        }
        if (intent == IntentType.HIGH_RISK || riskLevel == RiskLevel.HIGH) {
            candidates.add(candidate("risk_follow_up", "用户曾出现高风险表达，后续对话需要优先关注安全与支持。", 5));
        }
        if (intent == IntentType.KNOWLEDGE) {
            candidates.add(candidate("interest", "用户关注的心理知识主题：" + truncate(userMessage), 2));
        }
        return candidates;
    }

    private MemoryCandidate candidate(String type, String content, int importance) {
        MemoryCandidate candidate = new MemoryCandidate();
        candidate.setMemoryType(type);
        candidate.setContent(content);
        candidate.setImportance(importance);
        return candidate;
    }

    private void upsert(Long userId, Long sessionId, MemoryCandidate candidate) {
        String normalizedType = normalize(candidate.getMemoryType(), "note");
        String content = truncate(candidate.getContent());
        String memoryKey = normalizedType + ":" + sha256(content).substring(0, 24);
        LocalDateTime now = LocalDateTime.now();
        int updated = userMemoryMapper.update(null, new LambdaUpdateWrapper<UserMemory>()
                .eq(UserMemory::getUserId, userId)
                .eq(UserMemory::getMemoryKey, memoryKey)
                .set(UserMemory::getContent, content)
                .set(UserMemory::getImportance, candidate.getImportance() == null ? 3 : candidate.getImportance())
                .set(UserMemory::getSourceSessionId, sessionId)
                .set(UserMemory::getUpdateTime, now));
        if (updated > 0) {
            return;
        }
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryKey(memoryKey);
        memory.setMemoryType(normalizedType);
        memory.setContent(content);
        memory.setSourceSessionId(sessionId);
        memory.setImportance(candidate.getImportance() == null ? 3 : candidate.getImportance());
        memory.setCreateTime(now);
        memory.setUpdateTime(now);
        userMemoryMapper.insert(memory);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim().replaceAll("\\s+", " ");
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
