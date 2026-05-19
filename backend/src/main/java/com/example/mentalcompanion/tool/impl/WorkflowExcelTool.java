package com.example.mentalcompanion.tool.impl;

import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.service.ExcelExportService;
import com.example.mentalcompanion.tool.AiTool;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class WorkflowExcelTool implements AiTool {

    public static final String TOOL_NAME = "WorkflowExcelTool";

    private final ExcelExportService excelExportService;

    public WorkflowExcelTool(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        WorkflowRecord record = request.get("record", WorkflowRecord.class);
        return ToolResult.ok(excelExportService.appendWorkflowRecord(record).toString());
    }
}

