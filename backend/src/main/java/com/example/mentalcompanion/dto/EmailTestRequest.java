package com.example.mentalcompanion.dto;

import lombok.Data;

@Data
public class EmailTestRequest {
    private String receiver;
    private String subject;
    private String content;
}
