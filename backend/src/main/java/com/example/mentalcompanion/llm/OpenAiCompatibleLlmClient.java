package com.example.mentalcompanion.llm;

import com.example.mentalcompanion.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "openai")
public class OpenAiCompatibleLlmClient implements LlmClient, EmbeddingClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("temperature", 0.3);
        ArrayNode messages = body.putArray("messages");
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        JsonNode response = restTemplate.postForObject(url("/v1/chat/completions"), entity(body), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("OpenAI-compatible API returned empty response");
        }
        return response.path("choices").path(0).path("message").path("content").asText("");
    }

    @Override
    public String chatJson(String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("temperature", 0.0);
        body.put("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        ArrayNode messages = body.putArray("messages");
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        JsonNode response = restTemplate.postForObject(url("/v1/chat/completions"), entity(body), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("OpenAI-compatible API returned empty response");
        }
        return response.path("choices").path(0).path("message").path("content").asText("");
    }

    @Override
    public List<Double> embed(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.embeddingModel());
        body.put("input", text);
        JsonNode response = restTemplate.postForObject(url("/v1/embeddings"), entity(body), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("OpenAI-compatible embedding response is empty");
        }
        List<Double> embedding = new ArrayList<>();
        for (JsonNode value : response.path("data").path(0).path("embedding")) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private HttpEntity<ObjectNode> entity(ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            headers.setBearerAuth(properties.apiKey());
        }
        return new HttpEntity<>(body, headers);
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

