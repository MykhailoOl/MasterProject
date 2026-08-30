package com.example.masterproject.llm;

import com.example.masterproject.model.enums.LlmProvider;

public record LlmRuntimeSettings(
        LlmProvider provider,
        String model,
        double elicitationTemperature,
        double simplifyTemperature,
        int elicitationMaxTokens,
        int simplifyMaxTokens,
        int healthCheckMaxTokens,
        String healthCheckDescription) {

    public static LlmRuntimeSettings forProvider(LlmProvider provider) {
        return switch (provider) {
            case OPENAI -> new LlmRuntimeSettings(
                    LlmProvider.OPENAI,
                    "gpt-5-mini",
                    0.3,
                    0.2,
                    2000,
                    1000,
                    64,
                    "GET /v1/models");
            case ANTHROPIC -> new LlmRuntimeSettings(
                    LlmProvider.ANTHROPIC,
                    "claude-haiku-4-5",
                    0.3,
                    0.2,
                    2000,
                    1000,
                    16,
                    "POST /v1/messages (max_tokens=16)");
            case GEMINI -> new LlmRuntimeSettings(
                    LlmProvider.GEMINI,
                    "gemini-3.7-flash",
                    0.3,
                    0.2,
                    2000,
                    1000,
                    16,
                    "GET /v1beta/models");
            case GROK -> new LlmRuntimeSettings(
                    LlmProvider.GROK,
                    "grok-4.6",
                    0.3,
                    0.2,
                    2000,
                    1000,
                    64,
                    "GET /v1/models");
        };
    }
}
