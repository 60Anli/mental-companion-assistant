package com.example.mentalcompanion.rag;

import com.example.mentalcompanion.config.RetrievalProperties;
import com.example.mentalcompanion.dto.RagReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HybridRetrievalService {

    private final ChromaClient chromaClient;
    private final Bm25KnowledgeSearchService bm25KnowledgeSearchService;
    private final RetrievalProperties retrievalProperties;

    public HybridRetrievalService(
            ChromaClient chromaClient,
            Bm25KnowledgeSearchService bm25KnowledgeSearchService,
            RetrievalProperties retrievalProperties
    ) {
        this.chromaClient = chromaClient;
        this.bm25KnowledgeSearchService = bm25KnowledgeSearchService;
        this.retrievalProperties = retrievalProperties;
    }

    public List<RagReference> search(String query, int topK) {
        String mode = retrievalMode();
        if ("dense".equals(mode)) {
            return denseSearch(query, topK);
        }
        if ("sparse".equals(mode)) {
            return sparseSearch(query, topK);
        }
        return hybridSearch(query, topK);
    }

    private List<RagReference> hybridSearch(String query, int topK) {
        List<RagReference> denseResults = denseSearch(query, Math.max(topK, retrievalProperties.denseTopK()));
        List<Bm25SearchResult> sparseResults = bm25KnowledgeSearchService.search(
                query,
                Math.max(topK, retrievalProperties.sparseTopK())
        );
        Map<String, FusedRagCandidate> candidates = new LinkedHashMap<>();
        for (int i = 0; i < denseResults.size(); i++) {
            RagReference reference = denseResults.get(i);
            FusedRagCandidate candidate = candidates.computeIfAbsent(
                    key(reference.getDocumentName(), reference.getContent()),
                    ignored -> new FusedRagCandidate(reference.getDocumentName(), reference.getContent())
            );
            candidate.setDenseRank(i + 1);
            candidate.setDenseScore(reference.getScore());
        }
        for (Bm25SearchResult result : sparseResults) {
            FusedRagCandidate candidate = candidates.computeIfAbsent(
                    key(result.documentName(), result.content()),
                    ignored -> new FusedRagCandidate(result.documentName(), result.content())
            );
            candidate.setSparseRank(result.rank());
            candidate.setSparseScore((double) result.score());
        }
        int rrfK = Math.max(1, retrievalProperties.rrfK());
        return candidates.values().stream()
                .sorted(Comparator
                        .comparingDouble((FusedRagCandidate candidate) -> candidate.rrfScore(rrfK)).reversed()
                        .thenComparingInt(candidate -> candidate.getDenseRank() == 0 ? Integer.MAX_VALUE : candidate.getDenseRank())
                        .thenComparingInt(candidate -> candidate.getSparseRank() == 0 ? Integer.MAX_VALUE : candidate.getSparseRank()))
                .limit(Math.max(1, topK))
                .map(candidate -> new RagReference(
                        candidate.getDocumentName(),
                        candidate.getContent(),
                        candidate.rrfScore(rrfK)
                ))
                .toList();
    }

    private List<RagReference> denseSearch(String query, int topK) {
        try {
            return chromaClient.search(query, Math.max(1, topK));
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private List<RagReference> sparseSearch(String query, int topK) {
        List<Bm25SearchResult> results = bm25KnowledgeSearchService.search(query, Math.max(1, topK));
        List<RagReference> references = new ArrayList<>();
        for (Bm25SearchResult result : results) {
            references.add(new RagReference(result.documentName(), result.content(), (double) result.score()));
        }
        return references;
    }

    private String retrievalMode() {
        String mode = retrievalProperties.mode();
        if (mode == null || mode.isBlank()) {
            return "hybrid";
        }
        return mode.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String documentName, String content) {
        return normalize(documentName) + "\u001F" + normalize(content);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
