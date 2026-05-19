package com.example.mentalcompanion.rag;

public record DocumentChunk(String id, Long documentId, String documentName, int chunkIndex, String content) {
}

