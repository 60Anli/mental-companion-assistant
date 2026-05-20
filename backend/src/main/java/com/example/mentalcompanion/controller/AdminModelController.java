package com.example.mentalcompanion.controller;

import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.config.FineTuneProperties;
import com.example.mentalcompanion.config.LlmProperties;
import com.example.mentalcompanion.dto.ModelRuntimeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/model")
public class AdminModelController {

    private final LlmProperties llmProperties;
    private final FineTuneProperties fineTuneProperties;

    public AdminModelController(LlmProperties llmProperties, FineTuneProperties fineTuneProperties) {
        this.llmProperties = llmProperties;
        this.fineTuneProperties = fineTuneProperties;
    }

    @GetMapping("/runtime")
    public ApiResponse<ModelRuntimeResponse> runtime() {
        return ApiResponse.ok(new ModelRuntimeResponse(
                llmProperties.provider(),
                llmProperties.model(),
                llmProperties.embeddingModel(),
                fineTuneProperties.enabled(),
                fineTuneProperties.baseModel(),
                fineTuneProperties.adapterType(),
                fineTuneProperties.adapterPath(),
                fineTuneProperties.ollamaModel(),
                fineTuneProperties.trainingProfile()
        ));
    }
}

