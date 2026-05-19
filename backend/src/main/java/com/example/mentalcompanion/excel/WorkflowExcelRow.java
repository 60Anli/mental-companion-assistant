package com.example.mentalcompanion.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class WorkflowExcelRow {
    @ExcelProperty("记录ID")
    private Long recordId;
    @ExcelProperty("用户ID")
    private Long userId;
    @ExcelProperty("会话ID")
    private Long sessionId;
    @ExcelProperty("用户问题")
    private String userMessage;
    @ExcelProperty("意图类型")
    private String intent;
    @ExcelProperty("风险类型")
    private String riskType;
    @ExcelProperty("风险等级")
    private String riskLevel;
    @ExcelProperty("是否命中RAG")
    private String ragHit;
    @ExcelProperty("RAG参考片段")
    private String ragReferences;
    @ExcelProperty("AI回复")
    private String aiReply;
    @ExcelProperty("是否发送邮件")
    private String emailSent;
    @ExcelProperty("创建时间")
    private String createTime;

    public static WorkflowExcelRow from(WorkflowRecord record) {
        WorkflowExcelRow row = new WorkflowExcelRow();
        row.setRecordId(record.getId());
        row.setUserId(record.getUserId());
        row.setSessionId(record.getSessionId());
        row.setUserMessage(record.getUserMessage());
        row.setIntent(record.getIntent());
        row.setRiskType(record.getRiskType());
        row.setRiskLevel(record.getRiskLevel());
        row.setRagHit(Boolean.TRUE.equals(record.getRagHit()) ? "是" : "否");
        row.setRagReferences(record.getRagReferences());
        row.setAiReply(record.getAiReply());
        row.setEmailSent(Boolean.TRUE.equals(record.getEmailSent()) ? "是" : "否");
        row.setCreateTime(record.getCreateTime() == null ? "" : record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return row;
    }
}

