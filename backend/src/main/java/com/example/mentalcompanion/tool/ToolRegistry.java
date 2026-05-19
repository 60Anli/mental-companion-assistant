package com.example.mentalcompanion.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {

    private final Map<String, AiTool> tools;

    public ToolRegistry(List<AiTool> tools) {
        this.tools = tools.stream().collect(Collectors.toMap(AiTool::name, Function.identity()));
    }

    public ToolResult execute(String name, ToolRequest request) {
        AiTool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.fail("Tool not found: " + name);
        }
        return tool.execute(request);
    }
}

