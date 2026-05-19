package com.example.mentalcompanion.dto;

public record LoginResponse(String token, Long userId, String username, String role) {
}

