package com.example.mentalcompanion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_record")
public class RiskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String userMessage;
    private String riskType;
    private String riskLevel;
    private String aiReply;
    private Boolean handled;
    private LocalDateTime createTime;
}

