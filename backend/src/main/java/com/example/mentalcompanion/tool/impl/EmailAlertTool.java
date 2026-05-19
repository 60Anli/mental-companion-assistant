package com.example.mentalcompanion.tool.impl;

import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.service.EmailAlertService;
import com.example.mentalcompanion.tool.AiTool;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class EmailAlertTool implements AiTool {

    public static final String TOOL_NAME = "EmailAlertTool";

    private final EmailAlertService emailAlertService;

    public EmailAlertTool(EmailAlertService emailAlertService) {
        this.emailAlertService = emailAlertService;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        RiskRecord riskRecord = request.get("riskRecord", RiskRecord.class);
        boolean sent = emailAlertService.sendRiskAlert(riskRecord);
        return ToolResult.ok(sent);
    }
}

