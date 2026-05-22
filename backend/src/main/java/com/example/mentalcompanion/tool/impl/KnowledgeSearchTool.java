package com.example.mentalcompanion.tool.impl;

import com.example.mentalcompanion.config.ChromaProperties;
import com.example.mentalcompanion.dto.RagReference;
import com.example.mentalcompanion.rag.HybridRetrievalService;
import com.example.mentalcompanion.tool.AiTool;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeSearchTool implements AiTool {

    public static final String TOOL_NAME = "KnowledgeSearchTool";

    private final HybridRetrievalService hybridRetrievalService;
    private final ChromaProperties chromaProperties;

    public KnowledgeSearchTool(HybridRetrievalService hybridRetrievalService, ChromaProperties chromaProperties) {
        this.hybridRetrievalService = hybridRetrievalService;
        this.chromaProperties = chromaProperties;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String query = request.get("query", String.class);
        Integer topK = request.get("topK", Integer.class);
        try {
            List<RagReference> references = hybridRetrievalService.search(query, topK == null ? chromaProperties.topK() : topK);
            return ToolResult.ok(references);
        } catch (RuntimeException ex) {
            return ToolResult.ok("RAG search failed, continue without references: " + ex.getMessage(), List.of());
        }
    }
}
