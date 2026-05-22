package com.example.mentalcompanion.rag;

public record Bm25SearchResult(
        String documentName,
        String content,
        int rank,
        float score
) {
}
