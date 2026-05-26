package com.example.mentalcompanion.mcp;

import com.example.mentalcompanion.config.McpProperties;
import com.example.mentalcompanion.domain.entity.RiskRecord;
import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.tool.ToolRegistry;
import com.example.mentalcompanion.tool.ToolRequest;
import com.example.mentalcompanion.tool.ToolResult;
import com.example.mentalcompanion.tool.impl.EmailAlertTool;
import com.example.mentalcompanion.tool.impl.KnowledgeSearchTool;
import com.example.mentalcompanion.tool.impl.RiskRecordTool;
import com.example.mentalcompanion.tool.impl.WorkflowExcelTool;
import com.example.mentalcompanion.tool.impl.WorkflowRecordTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpServerController {

    private static final Logger log = LoggerFactory.getLogger(McpServerController.class);
    private static final String JSON_RPC_VERSION = "2.0";

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;

    public McpServerController(ToolRegistry toolRegistry, ObjectMapper objectMapper, McpProperties mcpProperties) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.mcpProperties = mcpProperties;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handle(
            @RequestBody McpJsonRpcRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!authorized(authorization)) {
            return ResponseEntity.status(401).body(error(request.getId(), -32001, "Unauthorized MCP request"));
        }
        return ResponseEntity.ok(dispatch(request));
    }

    private Map<String, Object> dispatch(McpJsonRpcRequest request) {
        Map<String, Object> params = request.getParams() == null ? Map.of() : request.getParams();
        return switch (request.getMethod()) {
            case "initialize" -> result(request.getId(), initializeResult());
            case "tools/list" -> result(request.getId(), Map.of("tools", toolDefinitions()));
            case "tools/call" -> result(request.getId(), callTool(params));
            case "notifications/initialized" -> result(request.getId(), Map.of());
            default -> error(request.getId(), -32601, "Method not found: " + request.getMethod());
        };
    }

    private Map<String, Object> initializeResult() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        return Map.of(
                "protocolVersion", mcpProperties.getServer().getProtocolVersion(),
                "capabilities", capabilities,
                "serverInfo", Map.of(
                        "name", mcpProperties.getServer().getName(),
                        "version", mcpProperties.getServer().getVersion()
                )
        );
    }

    private Map<String, Object> callTool(Map<String, Object> params) {
        String toolName = String.valueOf(params.get("name"));
        Map<String, Object> arguments = arguments(params.get("arguments"));
        ToolResult toolResult;
        try {
            toolResult = executeTool(toolName, arguments);
        } catch (RuntimeException ex) {
            log.warn("MCP tool call failed, tool={}", toolName, ex);
            toolResult = ToolResult.fail(ex.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of(
                "type", "text",
                "text", toJson(toolResult)
        )));
        result.put("structuredContent", toolResult);
        result.put("isError", !toolResult.isSuccess());
        return result;
    }

    private ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        log.info("MCP tool call: {}", toolName);
        return switch (toolName) {
            case McpToolNames.KNOWLEDGE_SEARCH -> toolRegistry.execute(
                    KnowledgeSearchTool.TOOL_NAME,
                    new ToolRequest(arguments)
            );
            case McpToolNames.SAVE_WORKFLOW_RECORD -> toolRegistry.execute(
                    WorkflowRecordTool.TOOL_NAME,
                    ToolRequest.of("record", convertArgument(arguments, "record", WorkflowRecord.class))
            );
            case McpToolNames.APPEND_WORKFLOW_EXCEL -> toolRegistry.execute(
                    WorkflowExcelTool.TOOL_NAME,
                    ToolRequest.of("record", convertArgument(arguments, "record", WorkflowRecord.class))
            );
            case McpToolNames.SAVE_RISK_RECORD -> toolRegistry.execute(
                    RiskRecordTool.TOOL_NAME,
                    ToolRequest.of("riskRecord", convertArgument(arguments, "riskRecord", RiskRecord.class))
            );
            case McpToolNames.SEND_EMAIL_ALERT -> toolRegistry.execute(
                    EmailAlertTool.TOOL_NAME,
                    ToolRequest.of("riskRecord", convertArgument(arguments, "riskRecord", RiskRecord.class))
            );
            default -> ToolResult.fail("Unknown MCP tool: " + toolName);
        };
    }

    private <T> T convertArgument(Map<String, Object> arguments, String key, Class<T> type) {
        Object value = arguments.containsKey(key) ? arguments.get(key) : arguments;
        return objectMapper.convertValue(value, type);
    }

    private Map<String, Object> arguments(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> toolDefinitions() {
        return List.of(
                tool(McpToolNames.KNOWLEDGE_SEARCH, "Hybrid RAG search over Chroma dense vectors and Lucene BM25.",
                        objectSchema(Map.of(
                                "query", stringSchema("User question for retrieval."),
                                "topK", integerSchema("Maximum reference chunks to return.")
                        ), List.of("query"))),
                tool(McpToolNames.SAVE_WORKFLOW_RECORD, "Persist a non-chat workflow record into MySQL.",
                        recordToolSchema("record", "workflow_record payload.")),
                tool(McpToolNames.APPEND_WORKFLOW_EXCEL, "Append a workflow record into the configured Excel file.",
                        recordToolSchema("record", "workflow_record payload.")),
                tool(McpToolNames.SAVE_RISK_RECORD, "Persist a high-risk conversation record into MySQL.",
                        recordToolSchema("riskRecord", "risk_record payload.")),
                tool(McpToolNames.SEND_EMAIL_ALERT, "Send a real SMTP high-risk alert email and write email log.",
                        recordToolSchema("riskRecord", "risk_record payload."))
        );
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", inputSchema
        );
    }

    private Map<String, Object> recordToolSchema(String property, String description) {
        return objectSchema(Map.of(property, Map.of(
                "type", "object",
                "description", description
        )), List.of(property));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> stringSchema(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> integerSchema(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private boolean authorized(String authorization) {
        String token = mcpProperties.getServer().getAccessToken();
        if (!StringUtils.hasText(token)) {
            return true;
        }
        return ("Bearer " + token).equals(authorization);
    }

    private Map<String, Object> result(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"success\":false,\"message\":\"Failed to serialize tool result\",\"data\":null}";
        }
    }
}
