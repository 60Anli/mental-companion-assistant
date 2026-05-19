package com.example.mentalcompanion.llm;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);

    String chatJson(String systemPrompt, String userPrompt);
}

