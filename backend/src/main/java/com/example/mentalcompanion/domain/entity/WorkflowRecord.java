package com.example.mentalcompanion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_record")
public class WorkflowRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String userMessage;
    private String intent;
    private String riskType;
    private String riskLevel;
    private Boolean ragHit;
    private String ragReferences;
    private String aiReply;
    private Boolean excelExported;
    private Boolean emailSent;
    private LocalDateTime createTime;
}

