package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.EmailAlertLog;
import com.example.mentalcompanion.dto.EmailTestRequest;
import com.example.mentalcompanion.mapper.EmailAlertLogMapper;
import com.example.mentalcompanion.service.EmailAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/email")
public class AdminEmailController {

    private final EmailAlertService emailAlertService;
    private final EmailAlertLogMapper emailAlertLogMapper;

    public AdminEmailController(EmailAlertService emailAlertService, EmailAlertLogMapper emailAlertLogMapper) {
        this.emailAlertService = emailAlertService;
        this.emailAlertLogMapper = emailAlertLogMapper;
    }

    @PostMapping("/test")
    public ApiResponse<Boolean> test(@RequestBody(required = false) EmailTestRequest request) {
        String receiver = request == null ? null : request.getReceiver();
        String subject = request == null ? null : request.getSubject();
        String content = request == null ? null : request.getContent();
        return ApiResponse.ok(emailAlertService.sendTest(receiver, subject, content));
    }

    @GetMapping("/logs")
    public ApiResponse<List<EmailAlertLog>> logs() {
        return ApiResponse.ok(emailAlertLogMapper.selectList(new LambdaQueryWrapper<EmailAlertLog>()
                .orderByDesc(EmailAlertLog::getCreateTime)));
    }
}
