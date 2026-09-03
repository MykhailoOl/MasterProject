package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class GrokLlmClient implements LlmClient {

    private static final List<String> PREFERRED_MODELS = List.of("grok-4.6", "grok-4.5", "grok-4.3");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppLog appLog;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.GROK);

    public GrokLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, AppLog appLog) {
        this.restClient = restClientBuilder.baseUrl("https://api.x.ai").build();
        this.objectMapper = objectMapper;
        this.appLog = appLog;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GROK;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        try {
            restClient.get()
                    .uri("/v1/api-key")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "Grok API key is valid.");
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", LlmErrorDetails.http("Grok", "API key check", "GET /v1/api-key", ex));
            return new LlmHealthResult(false, "Grok API key check failed.");
        } catch (Exception ex) {
            appLog.error("LLM", LlmErrorDetails.unexpected("Grok", "API key check", "GET /v1/api-key", ex));
            return new LlmHealthResult(false, "Grok API key check failed.");
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        for (String model : models()) {
            try {
                return completeChat(apiKey, model, systemPrompt, userPrompt, temperature, maxTokens);
            } catch (RestClientResponseException ex) {
                if (!LlmFailureMessages.canFallbackModel(ex)) {
                    throw completionFailure(ex, "POST /v1/chat/completions");
                }
                appLog.warn(
                        "LLM",
                        LlmErrorDetails.http(
                                "Grok",
                                "completion request for model " + model,
                                "POST /v1/chat/completions",
                                ex)
                                + " | action=trying another Grok model");
            }
        }
        try {
            return completeResponses(apiKey, settings.model(), systemPrompt, userPrompt, temperature, maxTokens);
        } catch (RestClientResponseException ex) {
            throw completionFailure(ex, "POST /v1/responses");
        } catch (Exception ex) {
            appLog.error(
                    "LLM",
                    LlmErrorDetails.unexpected("Grok", "completion request", "POST /v1/chat/completions", ex));
            throw new IllegalStateException("Grok could not generate a response. Please try again.", ex);
        }
    }

    private List<String> models() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        models.add(settings.model());
        models.addAll(PREFERRED_MODELS);
        return new ArrayList<>(models);
    }

    private String completeChat(
            String apiKey,
            String model,
            String systemPrompt,
            String userPrompt,
            double temperature,
            int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        appLog.info("LLM", "Calling Grok chat completions with model " + model + ".");
        String response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("Grok returned an empty response");
        }
        return content.asText().trim();
    }

    private String completeResponses(
            String apiKey,
            String model,
            String systemPrompt,
            String userPrompt,
            double temperature,
            int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("instructions", systemPrompt);
        body.put("max_output_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("store", false);
        body.putArray("input").addObject().put("role", "user").put("content", userPrompt);

        appLog.info("LLM", "Calling Grok responses with model " + model + ".");
        String response = restClient.post()
                .uri("/v1/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        String text = extractOutputText(objectMapper.readTree(response));
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Grok returned an empty response");
        }
        return text.trim();
    }

    private IllegalStateException completionFailure(RestClientResponseException error, String endpoint) {
        appLog.error("LLM", LlmErrorDetails.http("Grok", "completion request", endpoint, error));
        return new IllegalStateException(LlmFailureMessages.forHttp("Grok", error), error);
    }

    private String extractOutputText(JsonNode root) {
        if (root.hasNonNull("output_text") && !root.get("output_text").asText().isBlank()) {
            return root.get("output_text").asText();
        }
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                String type = part.path("type").asText();
                if ("output_text".equals(type) || "text".equals(type)) {
                    String value = part.path("text").asText("");
                    if (!value.isBlank()) {
                        if (!text.isEmpty()) {
                            text.append('\n');
                        }
                        text.append(value);
                    }
                }
            }
        }
        return text.isEmpty() ? null : text.toString();
    }
}
