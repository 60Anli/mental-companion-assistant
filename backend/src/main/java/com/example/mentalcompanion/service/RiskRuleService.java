package com.example.mentalcompanion.service;

import com.example.mentalcompanion.domain.enums.RiskLevel;
import com.example.mentalcompanion.dto.RuleRiskResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskRuleService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "想死", "不想活", "活不下去", "自杀", "结束生命", "伤害自己", "割腕", "跳楼",
            "没人救我", "我撑不住了", "撑不住了", "报复", "杀人", "伤害别人"
    );

    public RuleRiskResult detect(String message) {
        String normalized = message == null ? "" : message.replace(" ", "");
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return new RuleRiskResult(RiskLevel.HIGH, riskType(keyword), keyword);
            }
        }
        if (normalized.contains("崩溃") || normalized.contains("绝望") || normalized.contains("睡不着")) {
            return new RuleRiskResult(RiskLevel.MEDIUM, "distress", null);
        }
        return new RuleRiskResult(RiskLevel.LOW, "none", null);
    }

    private String riskType(String keyword) {
        if (keyword.contains("杀") || keyword.contains("报复") || keyword.contains("别人")) {
            return "violence";
        }
        return "self_harm";
    }
}

