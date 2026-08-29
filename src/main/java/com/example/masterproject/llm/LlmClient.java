package com.example.masterproject.llm;

import com.example.masterproject.model.enums.LlmProvider;

public interface LlmClient {

    LlmProvider provider();

    LlmHealthResult checkHealth(String apiKey);

    String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens);
}
