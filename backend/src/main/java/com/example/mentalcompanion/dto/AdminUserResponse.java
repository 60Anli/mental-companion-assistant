package com.example.mentalcompanion.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String realName,
        String college,
        String department,
        String email,
        String role,
        LocalDateTime createTime
) {
}
