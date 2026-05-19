package com.example.mentalcompanion.rag;

import com.example.mentalcompanion.config.ChromaProperties;
import com.example.mentalcompanion.dto.RagReference;
import com.example.mentalcompanion.llm.EmbeddingClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ChromaClient {

    private final ChromaProperties properties;
    private final EmbeddingClient embeddingClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private volatile String collectionId;

    public ChromaClient(
            ChromaProperties properties,
            EmbeddingClient embeddingClient,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void addChunks(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        String id = collectionId();
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode ids = body.putArray("ids");
        ArrayNode documents = body.putArray("documents");
        ArrayNode embeddings = body.putArray("embeddings");
        ArrayNode metadatas = body.putArray("metadatas");
        for (DocumentChunk chunk : chunks) {
            ids.add(chunk.id());
            documents.add(chunk.content());
            ArrayNode vector = objectMapper.createArrayNode();
            embeddingClient.embed(chunk.content()).forEach(vector::add);
            embeddings.add(vector);
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("documentId", chunk.documentId());
            metadata.put("documentName", chunk.documentName());
            metadata.put("chunkIndex", chunk.chunkIndex());
            metadatas.add(metadata);
        }
        restTemplate.postForObject(url("/api/v1/collections/" + id + "/add"), body, JsonNode.class);
    }

    public List<RagReference> search(String query, int topK) {
        String id = collectionId();
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode queryEmbeddings = body.putArray("query_embeddings");
        ArrayNode vector = objectMapper.createArrayNode();
        embeddingClient.embed(query).forEach(vector::add);
        queryEmbeddings.add(vector);
        body.put("n_results", Math.max(1, topK));
        body.putArray("include").add("documents").add("metadatas").add("distances");
        JsonNode response = restTemplate.postForObject(url("/api/v1/collections/" + id + "/query"), body, JsonNode.class);
        List<RagReference> references = new ArrayList<>();
        if (response == null) {
            return references;
        }
        JsonNode docs = response.path("documents").path(0);
        JsonNode metas = response.path("metadatas").path(0);
        JsonNode distances = response.path("distances").path(0);
        for (int i = 0; i < docs.size(); i++) {
            String documentName = metas.path(i).path("documentName").asText("unknown");
            Double score = distances.path(i).isMissingNode() ? null : distances.path(i).asDouble();
            references.add(new RagReference(documentName, docs.path(i).asText(), score));
        }
        return references;
    }

    private String collectionId() {
        if (collectionId != null) {
            return collectionId;
        }
        synchronized (this) {
            if (collectionId != null) {
                return collectionId;
            }
            collectionId = resolveCollectionId();
            return collectionId;
        }
    }

    private String resolveCollectionId() {
        try {
            JsonNode existing = restTemplate.getForObject(url("/api/v1/collections/" + properties.collection()), JsonNode.class);
            String id = existing == null ? null : existing.path("id").asText(null);
            if (id != null && !id.isBlank()) {
                return id;
            }
        } catch (RestClientException ignored) {
            // Collection may not exist yet.
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", properties.collection());
            body.set("metadata", objectMapper.createObjectNode().put("source", "mental-companion-assistant"));
            JsonNode created = restTemplate.postForObject(url("/api/v1/collections"), body, JsonNode.class);
            String id = created == null ? null : created.path("id").asText(null);
            if (id != null && !id.isBlank()) {
                return id;
            }
        } catch (RestClientException ignored) {
            // If another process created it concurrently, fall through to list lookup.
        }
        JsonNode all = restTemplate.getForObject(url("/api/v1/collections"), JsonNode.class);
        if (all != null && all.isArray()) {
            for (JsonNode item : all) {
                if (properties.collection().equals(item.path("name").asText())) {
                    return item.path("id").asText();
                }
            }
        }
        return UUID.nameUUIDFromBytes(properties.collection().getBytes()).toString();
    }

    private String url(String path) {
        return properties.baseUrl().replaceAll("/+$", "") + path;
    }
}

