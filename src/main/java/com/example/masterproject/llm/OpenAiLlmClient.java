package com.example.masterproject.llm;

import com.example.masterproject.model.enums.LlmProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.OPENAI);

    public OpenAiLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.OPENAI;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        try {
            restClient.get()
                    .uri("/v1/models")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "OpenAI API key is valid.");
        } catch (RestClientResponseException ex) {
            return new LlmHealthResult(false, "OpenAI check failed: HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            return new LlmHealthResult(false, "OpenAI check failed: " + ex.getMessage());
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", settings.model());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        try {
            String response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("OpenAI returned an empty response");
            }
            return content.asText().trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "OpenAI request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI request failed: " + ex.getMessage(), ex);
        }
    }

    public LlmRuntimeSettings settings() {
        return settings;
    }

    public List<String> displayParams() {
        return List.of(
                "model=" + settings.model(),
                "elicitation temperature=" + settings.elicitationTemperature(),
                "simplify temperature=" + settings.simplifyTemperature(),
                "health check=GET /v1/models");
    }
}
