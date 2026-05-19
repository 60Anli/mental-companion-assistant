package com.example.mentalcompanion.service;

import com.example.mentalcompanion.common.JsonUtils;
import com.example.mentalcompanion.domain.enums.IntentType;
import com.example.mentalcompanion.domain.enums.RiskLevel;
import com.example.mentalcompanion.dto.ClassificationResult;
import com.example.mentalcompanion.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class IntentRecognitionService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public IntentRecognitionService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public ClassificationResult recognize(String userMessage, String ragContext) {
        try {
            String raw = llmClient.chatJson(PromptTemplates.intentSystem(), PromptTemplates.intentUser(userMessage, ragContext));
            JsonNode json = objectMapper.readTree(JsonUtils.extractJsonObject(raw));
            ClassificationResult result = new ClassificationResult();
            result.setIntent(IntentType.safeValueOf(json.path("intent").asText(), IntentType.CHAT));
            result.setRiskLevel(RiskLevel.safeValueOf(json.path("riskLevel").asText(), RiskLevel.LOW));
            result.setRiskType(json.path("riskType").asText("none"));
            result.setReason(json.path("reason").asText(""));
            return result;
        } catch (Exception ex) {
            return fallback(userMessage);
        }
    }

    private ClassificationResult fallback(String message) {
        String text = message == null ? "" : message;
        ClassificationResult result = new ClassificationResult();
        if (text.matches(".*(想死|不想活|活不下去|自杀|结束生命|伤害自己|割腕|跳楼|杀人|伤害别人).*")) {
            result.setIntent(IntentType.HIGH_RISK);
            result.setRiskLevel(RiskLevel.HIGH);
            result.setRiskType("self_harm_or_violence");
            result.setReason("规则兜底识别到高风险关键词");
        } else if (text.matches(".*(什么|哪些|方法|概念|原理|怎么做|如何).*")) {
            result.setIntent(IntentType.KNOWLEDGE);
            result.setRiskLevel(RiskLevel.LOW);
            result.setRiskType("knowledge");
            result.setReason("LLM 不可用时按知识问答兜底");
        } else if (text.matches(".*(压力|焦虑|睡不着|难受|崩溃|关系|情绪|很累).*")) {
            result.setIntent(IntentType.CONSULT);
            result.setRiskLevel(text.contains("崩溃") || text.contains("睡不着") ? RiskLevel.MEDIUM : RiskLevel.LOW);
            result.setRiskType("stress");
            result.setReason("LLM 不可用时按情绪困扰兜底");
        } else {
            result.setIntent(IntentType.CHAT);
            result.setRiskLevel(RiskLevel.LOW);
            result.setRiskType("none");
            result.setReason("LLM 不可用时按闲聊兜底");
        }
        return result;
    }
}

