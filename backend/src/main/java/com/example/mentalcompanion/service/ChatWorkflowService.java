package com.example.mentalcompanion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.config.ChromaProperties;
import com.example.mentalcompanion.domain.entity.ChatMessage;
import com.example.mentalcompanion.domain.entity.ChatSession;
import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.domain.enums.IntentType;
import com.example.mentalcompanion.domain.enums.RiskLevel;
import com.example.mentalcompanion.dto.ChatSendRequest;
import com.example.mentalcompanion.dto.ChatSendResponse;
import com.example.mentalcompanion.dto.ClassificationResult;
import com.example.mentalcompanion.dto.RagReference;
import com.example.mentalcompanion.dto.RuleRiskResult;
import com.example.mentalcompanion.llm.LlmClient;
import com.example.mentalcompanion.mapper.ChatMessageMapper;
import com.example.mentalcompanion.mapper.ChatSessionMapper;
import com.example.mentalcompanion.mapper.WorkflowRecordMapper;
import com.example.mentalcompanion.tool.ToolRegistry;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import com.example.mentalcompanion.tool.impl.EmailAlertTool;
import com.example.mentalcompanion.tool.impl.KnowledgeSearchTool;
import com.example.mentalcompanion.tool.impl.RiskRecordTool;
import com.example.mentalcompanion.tool.impl.WorkflowExcelTool;
import com.example.mentalcompanion.tool.impl.WorkflowRecordTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatWorkflowService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final WorkflowRecordMapper workflowRecordMapper;
    private final ToolRegistry toolRegistry;
    private final IntentRecognitionService intentRecognitionService;
    private final RiskRuleService riskRuleService;
    private final ShortTermMemoryService shortTermMemoryService;
    private final LongTermMemoryService longTermMemoryService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final ChromaProperties chromaProperties;

    public ChatWorkflowService(
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper,
            WorkflowRecordMapper workflowRecordMapper,
            ToolRegistry toolRegistry,
            IntentRecognitionService intentRecognitionService,
            RiskRuleService riskRuleService,
            ShortTermMemoryService shortTermMemoryService,
            LongTermMemoryService longTermMemoryService,
            LlmClient llmClient,
            ObjectMapper objectMapper,
            ChromaProperties chromaProperties
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.workflowRecordMapper = workflowRecordMapper;
        this.toolRegistry = toolRegistry;
        this.intentRecognitionService = intentRecognitionService;
        this.riskRuleService = riskRuleService;
        this.shortTermMemoryService = shortTermMemoryService;
        this.longTermMemoryService = longTermMemoryService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.chromaProperties = chromaProperties;
    }

    @Transactional
    public ChatSendResponse processMessage(Long userId, ChatSendRequest request) {
        Long sessionId = ensureSession(userId, request.getSessionId(), request.getMessage());

        saveMessage(sessionId, "USER", request.getMessage(), null, null);

        List<String> actions = new ArrayList<>();
        actions.add("RAG_SEARCH");
        ToolResult ragResult = toolRegistry.execute(
                KnowledgeSearchTool.TOOL_NAME,
                ToolRequest.of("query", request.getMessage(), "topK", chromaProperties.topK())
        );
        @SuppressWarnings("unchecked")
        List<RagReference> references = ragResult.getData() instanceof List<?> list
                ? (List<RagReference>) list
                : List.of();
        String ragContext = ragContext(references);
        actions.add("LOAD_SHORT_TERM_MEMORY");
        String shortTermMemory = shortTermMemoryService.recentContext(userId, sessionId);
        actions.add("LOAD_LONG_TERM_MEMORY");
        String longTermMemory = longTermMemoryService.context(userId);

        ClassificationResult classification = intentRecognitionService.recognize(request.getMessage(), ragContext);
        RuleRiskResult ruleRisk = riskRuleService.detect(request.getMessage());
        RiskLevel finalRisk = RiskLevel.max(classification.getRiskLevel(), ruleRisk.riskLevel());
        IntentType finalIntent = classification.getIntent();
        String riskType = chooseRiskType(classification, ruleRisk);
        if (finalIntent == IntentType.HIGH_RISK || finalRisk == RiskLevel.HIGH) {
            finalIntent = IntentType.HIGH_RISK;
            finalRisk = RiskLevel.HIGH;
            actions.add("RULE_OR_LLM_HIGH_RISK_CHECK");
        }

        String reply = generateReply(finalIntent, request.getMessage(), ragContext, shortTermMemory, longTermMemory);
        saveMessage(sessionId, "ASSISTANT", reply, finalIntent.name(), finalRisk.name());
        actions.add("UPDATE_SHORT_TERM_MEMORY");
        shortTermMemoryService.appendTurn(userId, sessionId, request.getMessage(), reply);
        actions.add("UPDATE_LONG_TERM_MEMORY");
        longTermMemoryService.extractAndSave(userId, sessionId, request.getMessage(), reply, finalIntent, finalRisk);

        ChatSendResponse response = new ChatSendResponse();
        response.setSessionId(sessionId);
        response.setReply(reply);
        response.setIntent(finalIntent);
        response.setRiskLevel(finalRisk);
        response.setRiskType(riskType);
        response.setRagHit(!references.isEmpty());
        response.setReferences(references);
        response.setActions(actions);

        if (finalIntent == IntentType.CHAT) {
            return response;
        }

        WorkflowRecord workflowRecord = buildWorkflowRecord(userId, sessionId, request.getMessage(), finalIntent, finalRisk, riskType, references, reply);
        actions.add("SAVE_WORKFLOW_RECORD");
        workflowRecord = (WorkflowRecord) toolRegistry
                .execute(WorkflowRecordTool.TOOL_NAME, ToolRequest.of("record", workflowRecord))
                .getData();

        if (finalIntent == IntentType.HIGH_RISK) {
            actions.add("SAVE_RISK_RECORD");
            RiskRecord riskRecord = buildRiskRecord(userId, sessionId, request.getMessage(), finalRisk, riskType, reply);
            riskRecord = (RiskRecord) toolRegistry
                    .execute(RiskRecordTool.TOOL_NAME, ToolRequest.of("riskRecord", riskRecord))
                    .getData();
            actions.add("WRITE_EXCEL");
            toolRegistry.execute(WorkflowExcelTool.TOOL_NAME, ToolRequest.of("record", workflowRecord));
            actions.add("SEND_EMAIL_ALERT");
            Boolean emailSent = (Boolean) toolRegistry
                    .execute(EmailAlertTool.TOOL_NAME, ToolRequest.of("riskRecord", riskRecord))
                    .getData();
            workflowRecord.setEmailSent(Boolean.TRUE.equals(emailSent));
            workflowRecordMapper.updateById(workflowRecord);
            response.setActions(actions);
            return response;
        }

        actions.add("WRITE_EXCEL");
        toolRegistry.execute(WorkflowExcelTool.TOOL_NAME, ToolRequest.of("record", workflowRecord));
        response.setActions(actions);
        return response;
    }

    public List<ChatSession> sessions(Long userId) {
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getCreateTime));
    }

    public List<ChatMessage> messages(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("会话不存在");
        }
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime));
    }

    private Long ensureSession(Long userId, Long sessionId, String message) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return sessionId;
            }
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(message.length() > 18 ? message.substring(0, 18) : message);
        session.setCreateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session.getId();
    }

    private void saveMessage(Long sessionId, String role, String content, String intent, String riskLevel) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setIntent(intent);
        message.setRiskLevel(riskLevel);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
    }

    private String generateReply(IntentType intent, String message, String ragContext, String shortTermMemory, String longTermMemory) {
        try {
            return llmClient.chat(PromptTemplates.replySystem(intent), PromptTemplates.replyUser(message, ragContext, shortTermMemory, longTermMemory));
        } catch (RuntimeException ex) {
            return fallbackReply(intent);
        }
    }

    private String fallbackReply(IntentType intent) {
        return switch (intent) {
            case HIGH_RISK -> "我听见你现在很痛苦。请立刻联系身边可信任的人陪着你，并尽快联系当地紧急服务或专业心理危机热线。先让自己远离可能造成伤害的物品，别一个人扛着。";
            case CONSULT -> "听起来你最近承受了不少压力。可以先把今晚要做的事减到最少，做几轮缓慢呼吸，睡前把担心的事情写下来，留到明天再处理。如果这种状态持续，也建议联系专业支持。";
            case KNOWLEDGE -> "当前知识库中没有找到明确依据。一般来说，放松训练、规律作息、适度运动和记录情绪触发点，都常被用于缓解压力与焦虑体验。";
            case CHAT -> "我在呢。无聊的时候可以先挑一件很小的事做做，比如听首歌、倒杯水，或者和我随便聊两句。";
        };
    }

    private String ragContext(List<RagReference> references) {
        if (references.isEmpty()) {
            return "当前知识库没有找到明确依据。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < references.size(); i++) {
            RagReference reference = references.get(i);
            builder.append("[").append(i + 1).append("] 文档：")
                    .append(reference.getDocumentName()).append("\n")
                    .append(reference.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private String chooseRiskType(ClassificationResult classification, RuleRiskResult ruleRisk) {
        if (ruleRisk.riskLevel() != RiskLevel.LOW && ruleRisk.riskType() != null) {
            return ruleRisk.riskType();
        }
        return classification.getRiskType() == null || classification.getRiskType().isBlank()
                ? "none"
                : classification.getRiskType();
    }

    private WorkflowRecord buildWorkflowRecord(
            Long userId,
            Long sessionId,
            String userMessage,
            IntentType intent,
            RiskLevel riskLevel,
            String riskType,
            List<RagReference> references,
            String reply
    ) {
        WorkflowRecord record = new WorkflowRecord();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setUserMessage(userMessage);
        record.setIntent(intent.name());
        record.setRiskLevel(riskLevel.name());
        record.setRiskType(riskType);
        record.setRagHit(!references.isEmpty());
        record.setRagReferences(toJson(references));
        record.setAiReply(reply);
        record.setExcelExported(false);
        record.setEmailSent(false);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    private RiskRecord buildRiskRecord(Long userId, Long sessionId, String userMessage, RiskLevel riskLevel, String riskType, String reply) {
        RiskRecord riskRecord = new RiskRecord();
        riskRecord.setUserId(userId);
        riskRecord.setSessionId(sessionId);
        riskRecord.setUserMessage(userMessage);
        riskRecord.setRiskLevel(riskLevel.name());
        riskRecord.setRiskType(riskType);
        riskRecord.setAiReply(reply);
        riskRecord.setHandled(false);
        riskRecord.setCreateTime(LocalDateTime.now());
        return riskRecord;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
