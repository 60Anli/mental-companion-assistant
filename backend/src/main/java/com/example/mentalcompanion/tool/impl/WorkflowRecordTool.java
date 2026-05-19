package com.example.mentalcompanion.tool.impl;

import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.mapper.WorkflowRecordMapper;
import com.example.mentalcompanion.tool.AiTool;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRecordTool implements AiTool {

    public static final String TOOL_NAME = "WorkflowRecordTool";

    private final WorkflowRecordMapper workflowRecordMapper;

    public WorkflowRecordTool(WorkflowRecordMapper workflowRecordMapper) {
        this.workflowRecordMapper = workflowRecordMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        WorkflowRecord record = request.get("record", WorkflowRecord.class);
        workflowRecordMapper.insert(record);
        return ToolResult.ok(record);
    }
}

