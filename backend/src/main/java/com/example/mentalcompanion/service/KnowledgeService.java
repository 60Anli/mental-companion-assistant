package com.example.mentalcompanion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.domain.entity.KnowledgeChunk;
import com.example.mentalcompanion.domain.entity.KnowledgeDocument;
import com.example.mentalcompanion.mapper.KnowledgeChunkMapper;
import com.example.mentalcompanion.mapper.KnowledgeDocumentMapper;
import com.example.mentalcompanion.rag.Bm25KnowledgeSearchService;
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
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final DocumentChunker documentChunker;
    private final ChromaClient chromaClient;
    private final Bm25KnowledgeSearchService bm25KnowledgeSearchService;

    public KnowledgeService(
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper,
            DocumentChunker documentChunker,
            ChromaClient chromaClient,
            Bm25KnowledgeSearchService bm25KnowledgeSearchService
    ) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.documentChunker = documentChunker;
        this.chromaClient = chromaClient;
        this.bm25KnowledgeSearchService = bm25KnowledgeSearchService;
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

            List<KnowledgeChunk> persistedChunks = new ArrayList<>();
            List<DocumentChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
                knowledgeChunk.setDocumentId(document.getId());
                knowledgeChunk.setDocumentName(fileName);
                knowledgeChunk.setChunkIndex(i);
                knowledgeChunk.setContent(chunks.get(i));
                knowledgeChunk.setCreateTime(LocalDateTime.now());
                knowledgeChunkMapper.insert(knowledgeChunk);
                persistedChunks.add(knowledgeChunk);
                documentChunks.add(new DocumentChunk(
                        "chunk-" + knowledgeChunk.getId(),
                        document.getId(),
                        fileName,
                        i,
                        chunks.get(i)
                ));
            }
            chromaClient.addChunks(documentChunks);
            bm25KnowledgeSearchService.indexChunks(persistedChunks);
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
