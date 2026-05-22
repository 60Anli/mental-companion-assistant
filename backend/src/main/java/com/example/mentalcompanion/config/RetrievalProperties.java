package com.example.mentalcompanion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "retrieval")
public record RetrievalProperties(
        String mode,
        int denseTopK,
        int sparseTopK,
        int rrfK,
        String luceneIndexPath,
        boolean rebuildOnStartup
) {
}
