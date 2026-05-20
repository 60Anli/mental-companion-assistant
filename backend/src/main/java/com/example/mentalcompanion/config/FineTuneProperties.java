package com.example.mentalcompanion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fine-tune")
public record FineTuneProperties(
        boolean enabled,
        String baseModel,
        String adapterType,
        String adapterPath,
        String ollamaModel,
        String trainingProfile
) {
}

