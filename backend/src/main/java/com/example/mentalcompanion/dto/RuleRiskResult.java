package com.example.mentalcompanion.dto;

import com.example.mentalcompanion.domain.enums.RiskLevel;

public record RuleRiskResult(RiskLevel riskLevel, String riskType, String matchedKeyword) {
}

