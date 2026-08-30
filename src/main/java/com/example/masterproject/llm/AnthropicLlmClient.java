package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class AnthropicLlmClient implements LlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppLog appLog;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.ANTHROPIC);

    public AnthropicLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, AppLog appLog) {
        this.restClient = restClientBuilder.baseUrl("https://api.anthropic.com").build();
        this.objectMapper = objectMapper;
        this.appLog = appLog;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.ANTHROPIC;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", settings.model());
        body.put("max_tokens", settings.healthCheckMaxTokens());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", "ping");
        try {
            restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "Anthropic API key is valid. Using model " + settings.model() + ".");
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", "Anthropic health check failed: HTTP " + ex.getStatusCode().value());
            return new LlmHealthResult(false, "Anthropic check failed: HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            appLog.error("LLM", "Anthropic health check failed", ex);
            return new LlmHealthResult(false, "Anthropic check failed: " + ex.getMessage());
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", settings.model());
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("system", systemPrompt);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", userPrompt);

        try {
            appLog.info("LLM", "Calling Anthropic messages with model " + settings.model() + ".");
            String response = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new IllegalStateException("Anthropic returned an empty response");
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
            if (text.isEmpty()) {
                throw new IllegalStateException("Anthropic returned no text content");
            }
            return text.toString().trim();
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", "Anthropic request failed: HTTP " + ex.getStatusCode().value());
            throw new IllegalStateException(
                    "Anthropic request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            appLog.error("LLM", "Anthropic request failed", ex);
            throw new IllegalStateException("Anthropic request failed: " + ex.getMessage(), ex);
        }
    }
}
