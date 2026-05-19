package com.example.mentalcompanion.llm;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String text);
}

