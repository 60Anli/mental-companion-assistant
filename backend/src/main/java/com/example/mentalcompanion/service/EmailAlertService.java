package com.example.mentalcompanion.service;

import com.example.mentalcompanion.config.AppProperties;
import com.example.mentalcompanion.domain.entity.EmailAlertLog;
import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.mapper.EmailAlertLogMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailAlertService {

    private final AppProperties appProperties;
    private final JavaMailSender mailSender;
    private final EmailAlertLogMapper emailAlertLogMapper;

    public EmailAlertService(AppProperties appProperties, JavaMailSender mailSender, EmailAlertLogMapper emailAlertLogMapper) {
        this.appProperties = appProperties;
        this.mailSender = mailSender;
        this.emailAlertLogMapper = emailAlertLogMapper;
    }

    public boolean sendRiskAlert(RiskRecord riskRecord) {
        String subject = "心理陪伴助手高风险预警";
        String content = """
                检测到高风险会话，请管理员尽快关注。

                用户ID：%s
                会话ID：%s
                风险等级：%s
                风险类型：%s
                用户内容：%s
                AI回复：%s
                """.formatted(
                riskRecord.getUserId(),
                riskRecord.getSessionId(),
                riskRecord.getRiskLevel(),
                riskRecord.getRiskType(),
                riskRecord.getUserMessage(),
                riskRecord.getAiReply()
        );
        return send(riskRecord.getId(), appProperties.mail().alertReceiver(), subject, content);
    }

    public boolean sendTest(String receiver) {
        String target = receiver == null || receiver.isBlank() ? appProperties.mail().alertReceiver() : receiver;
        return send(null, target, "心理陪伴助手邮件测试", "这是一封邮件预警通道测试邮件。");
    }

    private boolean send(Long riskRecordId, String receiver, String subject, String content) {
        if (!appProperties.mail().enabled()) {
            saveLog(riskRecordId, receiver, subject, content, "SKIPPED", "app.mail.enabled=false");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(receiver);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            saveLog(riskRecordId, receiver, subject, content, "SUCCESS", null);
            return true;
        } catch (RuntimeException ex) {
            saveLog(riskRecordId, receiver, subject, content, "FAILED", ex.getMessage());
            return false;
        }
    }

    private void saveLog(Long riskRecordId, String receiver, String subject, String content, String status, String error) {
        EmailAlertLog log = new EmailAlertLog();
        log.setRiskRecordId(riskRecordId);
        log.setReceiver(receiver);
        log.setSubject(subject);
        log.setContent(content);
        log.setSendStatus(status);
        log.setErrorMessage(error);
        log.setCreateTime(LocalDateTime.now());
        emailAlertLogMapper.insert(log);
    }
}

