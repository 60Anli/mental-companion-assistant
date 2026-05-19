package com.example.mentalcompanion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagReference {
    private String documentName;
    private String content;
    private Double score;
}

