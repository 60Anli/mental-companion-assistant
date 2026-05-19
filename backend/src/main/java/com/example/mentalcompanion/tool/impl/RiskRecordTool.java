package com.example.mentalcompanion.tool.impl;

import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.mapper.RiskRecordMapper;
import com.example.mentalcompanion.tool.AiTool;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class RiskRecordTool implements AiTool {

    public static final String TOOL_NAME = "RiskRecordTool";

    private final RiskRecordMapper riskRecordMapper;

    public RiskRecordTool(RiskRecordMapper riskRecordMapper) {
        this.riskRecordMapper = riskRecordMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        RiskRecord riskRecord = request.get("riskRecord", RiskRecord.class);
        riskRecordMapper.insert(riskRecord);
        return ToolResult.ok(riskRecord);
    }
}

