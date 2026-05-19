package com.example.mentalcompanion.controller;

import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.KnowledgeDocument;
import com.example.mentalcompanion.service.KnowledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
public class AdminKnowledgeController {

    private final KnowledgeService knowledgeService;

    public AdminKnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/upload")
    public ApiResponse<KnowledgeDocument> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(knowledgeService.upload(file));
    }

    @GetMapping("/list")
    public ApiResponse<List<KnowledgeDocument>> list() {
        return ApiResponse.ok(knowledgeService.list());
    }
}

