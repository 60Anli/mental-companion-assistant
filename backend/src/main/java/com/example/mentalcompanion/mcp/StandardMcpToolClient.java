package com.example.mentalcompanion.mcp;

import com.example.mentalcompanion.config.McpProperties;
import com.example.mentalcompanion.tool.ToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class StandardMcpToolClient implements McpToolClient {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public StandardMcpToolClient(RestTemplate restTemplate, ObjectMapper objectMapper, McpProperties mcpProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.mcpProperties = mcpProperties;
    }

    @Override
    public ToolResult callTool(String name, Object arguments) {
        if (!mcpProperties.isEnabled()) {
            throw new IllegalStateException("MCP is disabled");
        }
        ensureInitialized();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments == null ? Map.of() : arguments);

        JsonNode response = post("tools/call", params);
        JsonNode result = response.path("result");
        JsonNode structuredContent = result.path("structuredContent");
        try {
            if (!structuredContent.isMissingNode() && !structuredContent.isNull()) {
                return objectMapper.treeToValue(structuredContent, ToolResult.class);
            }
            JsonNode content = result.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return objectMapper.readValue(content.get(0).path("text").asText(), ToolResult.class);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse MCP tool result", ex);
        }
        throw new IllegalStateException("MCP tool result does not contain structuredContent");
    }

    @Override
    public List<String> listTools() {
        ensureInitialized();
        JsonNode response = post("tools/list", Map.of());
        List<String> names = new ArrayList<>();
        JsonNode tools = response.path("result").path("tools");
        if (tools.isArray()) {
            tools.forEach(tool -> names.add(tool.path("name").asText()));
        }
        return names;
    }

    private void ensureInitialized() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", mcpProperties.getServer().getProtocolVersion());
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of(
                "name", "mental-companion-backend",
                "version", "1.0.0"
        ));
        try {
            post("initialize", params);
            post("notifications/initialized", Map.of());
        } catch (RuntimeException ex) {
            initialized.set(false);
            throw ex;
        }
    }

    private JsonNode post(String method, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", JSON_RPC_VERSION);
        payload.put("id", idGenerator.getAndIncrement());
        payload.put("method", method);
        payload.put("params", params);

        JsonNode response = restTemplate.exchange(
                mcpProperties.getClient().getUrl(),
                HttpMethod.POST,
                new HttpEntity<>(payload, headers()),
                JsonNode.class
        ).getBody();
        if (response == null) {
            throw new IllegalStateException("MCP server returned empty response");
        }
        JsonNode error = response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new IllegalStateException("MCP error: " + error.path("message").asText(error.toString()));
        }
        return response;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(PROTOCOL_HEADER, mcpProperties.getServer().getProtocolVersion());
        String accessToken = mcpProperties.getClient().getAccessToken();
        if (StringUtils.hasText(accessToken)) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }
}
