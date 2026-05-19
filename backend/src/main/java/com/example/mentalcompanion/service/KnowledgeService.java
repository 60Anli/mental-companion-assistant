package com.example.mentalcompanion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.domain.entity.KnowledgeDocument;
import com.example.mentalcompanion.mapper.KnowledgeDocumentMapper;
import com.example.mentalcompanion.rag.ChromaClient;
import com.example.mentalcompanion.rag.DocumentChunk;
import com.example.mentalcompanion.rag.DocumentChunker;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunker documentChunker;
    private final ChromaClient chromaClient;

    public KnowledgeService(
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            DocumentChunker documentChunker,
            ChromaClient chromaClient
    ) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentChunker = documentChunker;
        this.chromaClient = chromaClient;
    }

    public KnowledgeDocument upload(MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "knowledge.txt" : file.getOriginalFilename();
        if (!fileName.endsWith(".txt") && !fileName.endsWith(".md")) {
            throw new IllegalArgumentException("仅支持上传 txt / md 文档");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> chunks = documentChunker.split(content);
            KnowledgeDocument document = new KnowledgeDocument();
            document.setFileName(fileName);
            document.setContent(content);
            document.setChunkCount(chunks.size());
            document.setCreateTime(LocalDateTime.now());
            knowledgeDocumentMapper.insert(document);

            List<DocumentChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                documentChunks.add(new DocumentChunk(
                        "doc-" + document.getId() + "-chunk-" + i,
                        document.getId(),
                        fileName,
                        i,
                        chunks.get(i)
                ));
            }
            chromaClient.addChunks(documentChunks);
            return document;
        } catch (Exception ex) {
            throw new IllegalStateException("知识库文档处理失败: " + ex.getMessage(), ex);
        }
    }

    public List<KnowledgeDocument> list() {
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .select(KnowledgeDocument::getId, KnowledgeDocument::getFileName, KnowledgeDocument::getChunkCount, KnowledgeDocument::getCreateTime)
                .orderByDesc(KnowledgeDocument::getCreateTime));
    }
}

