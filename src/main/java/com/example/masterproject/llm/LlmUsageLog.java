package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import tools.jackson.databind.JsonNode;

final class LlmUsageLog {
    private LlmUsageLog() {}
    static void record(AppLog log, String requestedModel, JsonNode root) {
        LlmCallTrace.response(root);
        JsonNode usage = root.has("usageMetadata") ? root.path("usageMetadata") : root.path("usage");
        String actual = root.path("model").asText(root.path("modelVersion").asText("unknown"));
        log.info("LLM_USAGE", "requested_model=" + requestedModel + " actual_model=" + actual
                + " input_tokens=" + first(usage, "input_tokens", "prompt_tokens", "promptTokenCount")
                + " output_tokens=" + first(usage, "output_tokens", "completion_tokens", "candidatesTokenCount")
                + " reasoning_tokens=" + first(usage, "thoughtsTokenCount")
                + " total_tokens=" + first(usage, "total_tokens", "totalTokenCount"));
    }
    private static long first(JsonNode node, String... keys) {
        for (String key : keys) if (node.path(key).isNumber()) return node.path(key).asLong();
        return -1;
    }
}
