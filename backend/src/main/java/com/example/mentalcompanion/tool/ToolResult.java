package com.example.mentalcompanion.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private boolean success;
    private String message;
    private Object data;

    public static ToolResult ok(Object data) {
        return new ToolResult(true, "OK", data);
    }

    public static ToolResult ok(String message, Object data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult fail(String message) {
        return new ToolResult(false, message, null);
    }
}

