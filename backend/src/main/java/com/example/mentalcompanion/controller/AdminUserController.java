package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.SysUser;
import com.example.mentalcompanion.dto.AdminUserResponse;
import com.example.mentalcompanion.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final SysUserMapper sysUserMapper;

    public AdminUserController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        List<AdminUserResponse> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getCreateTime))
                .stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getCollege(),
                        user.getDepartment(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreateTime()
                ))
                .toList();
        return ApiResponse.ok(users);
    }
}
