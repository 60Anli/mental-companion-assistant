package com.example.mentalcompanion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chroma")
public record ChromaProperties(String baseUrl, String collection, int topK) {
}

