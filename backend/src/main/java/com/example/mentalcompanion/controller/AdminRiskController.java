package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.mapper.RiskRecordMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/risk-records")
public class AdminRiskController {

    private final RiskRecordMapper riskRecordMapper;

    public AdminRiskController(RiskRecordMapper riskRecordMapper) {
        this.riskRecordMapper = riskRecordMapper;
    }

    @GetMapping
    public ApiResponse<List<RiskRecord>> list() {
        return ApiResponse.ok(riskRecordMapper.selectList(new LambdaQueryWrapper<RiskRecord>()
                .orderByDesc(RiskRecord::getCreateTime)));
    }
}

