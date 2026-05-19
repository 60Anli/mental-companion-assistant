package com.example.mentalcompanion.dto;

import com.example.mentalcompanion.domain.enums.IntentType;
import com.example.mentalcompanion.domain.enums.RiskLevel;
import lombok.Data;

@Data
public class ClassificationResult {
    private IntentType intent = IntentType.CHAT;
    private RiskLevel riskLevel = RiskLevel.LOW;
    private String riskType = "none";
    private String reason = "";
}

