package com.example.mentalcompanion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {
    private Long sessionId;
    @NotBlank
    private String message;
}

