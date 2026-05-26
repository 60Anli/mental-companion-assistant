package com.example.mentalcompanion.mcp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class McpJsonRpcRequest {
    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private Map<String, Object> params = new LinkedHashMap<>();
}
