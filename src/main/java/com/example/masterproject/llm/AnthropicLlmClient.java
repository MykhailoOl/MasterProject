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
        try {
            restClient.get()
                    .uri("/v1/models")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "Anthropic API key is valid.");
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", LlmErrorDetails.http("Anthropic", "API key check", "GET /v1/models", ex));
            return new LlmHealthResult(false, "Anthropic API key check failed.");
        } catch (Exception ex) {
            appLog.error("LLM", LlmErrorDetails.unexpected("Anthropic", "API key check", "GET /v1/models", ex));
            return new LlmHealthResult(false, "Anthropic API key check failed.");
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
            appLog.error("LLM", LlmErrorDetails.http("Anthropic", "completion request", "POST /v1/messages", ex));
            throw new IllegalStateException("Anthropic could not generate a response. Please try again.", ex);
        } catch (Exception ex) {
            appLog.error(
                    "LLM",
                    LlmErrorDetails.unexpected("Anthropic", "completion request", "POST /v1/messages", ex));
            throw new IllegalStateException("Anthropic could not generate a response. Please try again.", ex);
        }
    }
}
