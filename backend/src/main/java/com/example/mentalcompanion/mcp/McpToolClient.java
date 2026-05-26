package com.example.mentalcompanion.mcp;

import com.example.mentalcompanion.tool.ToolResult;

import java.util.List;

public interface McpToolClient {

    ToolResult callTool(String name, Object arguments);

    List<String> listTools();
}
