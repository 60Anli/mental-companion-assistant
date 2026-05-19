package com.example.mentalcompanion.security;

public record JwtUserPrincipal(Long userId, String username, String role) {
}

