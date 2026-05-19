package com.example.mentalcompanion.tool;

public interface AiTool {
    String name();

    ToolResult execute(ToolRequest request);
}

