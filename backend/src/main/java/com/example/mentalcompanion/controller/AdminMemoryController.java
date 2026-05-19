package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.UserMemory;
import com.example.mentalcompanion.mapper.UserMemoryMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/memories")
public class AdminMemoryController {

    private final UserMemoryMapper userMemoryMapper;

    public AdminMemoryController(UserMemoryMapper userMemoryMapper) {
        this.userMemoryMapper = userMemoryMapper;
    }

    @GetMapping
    public ApiResponse<List<UserMemory>> list() {
        return ApiResponse.ok(userMemoryMapper.selectList(new LambdaQueryWrapper<UserMemory>()
                .orderByDesc(UserMemory::getUpdateTime)));
    }
}

