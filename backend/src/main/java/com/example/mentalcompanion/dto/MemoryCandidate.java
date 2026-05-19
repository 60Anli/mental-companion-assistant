package com.example.mentalcompanion.dto;

import lombok.Data;

@Data
public class MemoryCandidate {
    private String memoryType;
    private String content;
    private Integer importance;
}

