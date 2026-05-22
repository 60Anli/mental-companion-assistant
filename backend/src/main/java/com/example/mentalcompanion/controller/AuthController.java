package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.SysUser;
import com.example.mentalcompanion.dto.LoginRequest;
import com.example.mentalcompanion.dto.LoginResponse;
import com.example.mentalcompanion.dto.RegisterRequest;
import com.example.mentalcompanion.mapper.SysUserMapper;
import com.example.mentalcompanion.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok(new LoginResponse(token, user.getId(), user.getUsername(), user.getRole()));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername().trim();
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setCollege(request.getCollege().trim());
        user.setEmail(request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim());
        user.setRole("USER");
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok("注册成功", new LoginResponse(token, user.getId(), user.getUsername(), user.getRole()));
    }
}
