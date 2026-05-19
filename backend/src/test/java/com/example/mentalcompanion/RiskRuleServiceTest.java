package com.example.mentalcompanion;

import com.example.mentalcompanion.domain.enums.RiskLevel;
import com.example.mentalcompanion.service.RiskRuleService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRuleServiceTest {

    private final RiskRuleService riskRuleService = new RiskRuleService();

    @Test
    void detectsHighRiskKeyword() {
        assertThat(riskRuleService.detect("我真的活不下去了").riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void detectsLowRiskChat() {
        assertThat(riskRuleService.detect("你好，今天有点无聊").riskLevel()).isEqualTo(RiskLevel.LOW);
    }
}

