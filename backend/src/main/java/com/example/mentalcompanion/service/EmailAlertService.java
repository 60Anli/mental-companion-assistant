package com.example.mentalcompanion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.config.AppProperties;
import com.example.mentalcompanion.domain.entity.EmailAlertLog;
import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.domain.entity.SysUser;
import com.example.mentalcompanion.mapper.EmailAlertLogMapper;
import com.example.mentalcompanion.mapper.SysUserMapper;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailAlertService {

    private final AppProperties appProperties;
    private final JavaMailSender mailSender;
    private final EmailAlertLogMapper emailAlertLogMapper;
    private final SysUserMapper sysUserMapper;

    public EmailAlertService(
            AppProperties appProperties,
            JavaMailSender mailSender,
            EmailAlertLogMapper emailAlertLogMapper,
            SysUserMapper sysUserMapper
    ) {
        this.appProperties = appProperties;
        this.mailSender = mailSender;
        this.emailAlertLogMapper = emailAlertLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public boolean sendRiskAlert(RiskRecord riskRecord) {
        SysUser student = sysUserMapper.selectById(riskRecord.getUserId());
        String studentName = firstNonBlank(
                student == null ? null : student.getRealName(),
                student == null ? null : student.getUsername(),
                "未知学生"
        );
        String studentCollege = firstNonBlank(student == null ? null : student.getCollege(), "未配置学院");
        String studentUsername = firstNonBlank(student == null ? null : student.getUsername(), "未配置账号");
        String studentEmail = firstNonBlank(student == null ? null : student.getEmail(), "未配置邮箱");
        String teacherName = firstNonBlank(appProperties.mail().teacherName(), "未配置负责老师");
        String teacherDepartment = firstNonBlank(appProperties.mail().teacherDepartment(), "未配置部门");
        String receiver = resolveAlertReceiver();

        String subject = "心理陪伴助手高风险预警 - " + studentName;
        String content = """
                检测到高风险会话，请负责老师尽快关注。

                【学生信息】
                学生姓名：%s
                学院：%s
                学生账号：%s
                学生邮箱：%s
                学生ID：%s

                【负责老师】
                老师姓名：%s
                所属部门：%s
                预警接收邮箱：%s

                【风险信息】
                会话ID：%s
                风险等级：%s
                风险类型：%s

                【学生原文】
                %s

                【AI安全回复】
                %s
                """.formatted(
                studentName,
                studentCollege,
                studentUsername,
                studentEmail,
                riskRecord.getUserId(),
                teacherName,
                teacherDepartment,
                receiver,
                riskRecord.getSessionId(),
                riskRecord.getRiskLevel(),
                firstNonBlank(riskRecord.getRiskType(), "未分类"),
                riskRecord.getUserMessage(),
                riskRecord.getAiReply()
        );
        return send(riskRecord.getId(), receiver, subject, content);
    }

    public boolean sendTest(String receiver) {
        return sendTest(receiver, null, null);
    }

    public boolean sendTest(String receiver, String subject, String content) {
        String target = receiver == null || receiver.isBlank() ? appProperties.mail().alertReceiver() : receiver;
        String mailSubject = subject == null || subject.isBlank() ? "心理陪伴助手邮件测试" : subject;
        String mailContent = content == null || content.isBlank() ? "这是一封邮件预警通道测试邮件。" : content;
        return send(null, target, mailSubject, mailContent);
    }

    private boolean send(Long riskRecordId, String receiver, String subject, String content) {
        if (!appProperties.mail().enabled()) {
            saveLog(riskRecordId, receiver, subject, content, "SKIPPED", "app.mail.enabled=false");
            return false;
        }
        String validationError = validateMailConfig(receiver);
        if (validationError != null) {
            saveLog(riskRecordId, receiver, subject, content, "FAILED", validationError);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.mail().from());
            message.setTo(receiver);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            saveLog(riskRecordId, receiver, subject, content, "SUCCESS", null);
            return true;
        } catch (MailException ex) {
            saveLog(riskRecordId, receiver, subject, content, "FAILED", ex.getMessage());
            return false;
        }
    }

    private String validateMailConfig(String receiver) {
        if (receiver == null || receiver.isBlank()) {
            return "Missing mail receiver";
        }
        if (receiver.trim().toLowerCase().endsWith("@example.com")) {
            return "Missing real mail receiver, current receiver is an example address";
        }
        if (appProperties.mail().from() == null || appProperties.mail().from().isBlank()) {
            return "Missing app.mail.from";
        }
        return null;
    }

    private String resolveAlertReceiver() {
        String configuredReceiver = appProperties.mail().alertReceiver();
        if (configuredReceiver != null && !configuredReceiver.isBlank()
                && !"admin@example.com".equalsIgnoreCase(configuredReceiver.trim())) {
            return configuredReceiver.trim();
        }
        SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ADMIN")
                .last("LIMIT 1"));
        String adminEmail = admin == null ? null : admin.getEmail();
        if (adminEmail != null && !adminEmail.isBlank()) {
            return adminEmail.trim();
        }
        return configuredReceiver;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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
