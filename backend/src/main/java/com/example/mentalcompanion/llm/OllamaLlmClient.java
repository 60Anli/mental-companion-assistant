package com.example.mentalcompanion.llm;

import com.example.mentalcompanion.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaLlmClient implements LlmClient, EmbeddingClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaLlmClient(LlmProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        JsonNode response = restTemplate.postForObject(url("/api/chat"), body, JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Ollama returned empty response");
        }
        return response.path("message").path("content").asText("");
    }

    @Override
    public String chatJson(String systemPrompt, String userPrompt) {
        return chat(systemPrompt + "\n请严格只输出 JSON。", userPrompt);
    }

    @Override
    public List<Double> embed(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.embeddingModel());
        body.put("prompt", text);
        JsonNode response = restTemplate.postForObject(url("/api/embeddings"), body, JsonNode.class);
        if (response == null || !response.has("embedding")) {
            throw new IllegalStateException("Ollama embedding response is invalid");
        }
        List<Double> embedding = new ArrayList<>();
        for (JsonNode value : response.path("embedding")) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private String url(String path) {
        return properties.baseUrl().replaceAll("/+$", "") + path;
    }
}

