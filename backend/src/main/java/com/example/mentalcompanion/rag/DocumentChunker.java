package com.example.mentalcompanion.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int MAX_CHARS = 600;
    private static final int OVERLAP = 80;

    public List<String> split(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        if (normalized.isBlank()) {
            return chunks;
        }
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + MAX_CHARS, normalized.length());
            if (end < normalized.length()) {
                int paragraphBreak = normalized.lastIndexOf("\n\n", end);
                if (paragraphBreak > start + 200) {
                    end = paragraphBreak;
                }
            }
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - OVERLAP);
        }
        return chunks;
    }
}

