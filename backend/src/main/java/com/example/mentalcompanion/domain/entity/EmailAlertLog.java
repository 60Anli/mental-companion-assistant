package com.example.mentalcompanion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_alert_log")
public class EmailAlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long riskRecordId;
    private String receiver;
    private String subject;
    private String content;
    private String sendStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}

