package com.example.mentalcompanion.dto;

public record ModelRuntimeResponse(
        String provider,
        String chatModel,
        String embeddingModel,
        boolean fineTuneEnabled,
        String fineTuneBaseModel,
        String adapterType,
        String adapterPath,
        String ollamaModel,
        String trainingProfile
) {
}

