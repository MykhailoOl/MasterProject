package com.example.masterproject.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public final class LlmCallTrace implements AutoCloseable {
    private static final ThreadLocal<LlmCallTrace> CURRENT = new ThreadLocal<>();
    private final List<Map<String, Object>> attempts = new ArrayList<>();
    private LlmCallTrace() { CURRENT.set(this); }
    public static LlmCallTrace start() { return new LlmCallTrace(); }
    public List<Map<String, Object>> attempts() { return List.copyOf(attempts); }
    public static void request(String model, Double temperature, String policy, int maxTokens) {
        LlmCallTrace trace = CURRENT.get();
        if (trace == null) return;
        Map<String, Object> attempt = new LinkedHashMap<>();
        attempt.put("requestedModel", model);
        attempt.put("sentTemperature", temperature);
        attempt.put("temperaturePolicy", policy);
        attempt.put("maxOutputTokens", maxTokens);
        trace.attempts.add(attempt);
    }
    public static void response(JsonNode root) {
        LlmCallTrace trace = CURRENT.get();
        if (trace == null || trace.attempts.isEmpty()) return;
        Map<String, Object> attempt = trace.attempts.getLast();
        String actual = root.path("model").asText(root.path("modelVersion").asText(""));
        attempt.put("actualModel", actual.isBlank() ? null : actual);
        attempt.put("finishReason", root.path("choices").path(0).path("finish_reason").asText(
                root.path("stop_reason").asText(root.path("candidates").path(0).path("finishReason").asText(
                        root.path("status").asText("unknown")))));
        JsonNode usage = root.has("usageMetadata") ? root.path("usageMetadata") : root.path("usage");
        for (String key : List.of("input_tokens", "output_tokens", "prompt_tokens", "completion_tokens", "total_tokens",
                "promptTokenCount", "candidatesTokenCount", "thoughtsTokenCount", "totalTokenCount")) {
            if (usage.path(key).isNumber()) attempt.put(key, usage.path(key).asLong());
        }
    }
    public static void failure(Throwable error) {
        LlmCallTrace trace = CURRENT.get();
        if (trace == null) return;
        if (trace.attempts.isEmpty()) trace.attempts.add(new LinkedHashMap<>());
        Map<String, Object> attempt = trace.attempts.getLast();
        attempt.put("failureKind", failureKind(error));
        attempt.put("exception", error.getClass().getSimpleName());
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof org.springframework.web.client.RestClientResponseException http) {
                attempt.put("providerError", com.example.masterproject.logging.DiagnosticSanitizer.http(http));
                break;
            }
        }
    }
    public static String failureKind(Throwable error) {
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof org.springframework.web.client.RestClientResponseException http) {
                return http.getStatusCode().value() == 429 ? "QUOTA_OR_RATE_LIMIT" : "HTTP_" + http.getStatusCode().value();
            }
            if (cause.getMessage() != null && cause.getMessage().contains("incomplete response")) return "INCOMPLETE_RESPONSE";
        }
        return "PROVIDER_OR_RESPONSE_FAILURE";
    }
    @Override public void close() { CURRENT.remove(); }
}
