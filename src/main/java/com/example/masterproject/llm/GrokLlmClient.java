package com.example.masterproject.llm;

import com.example.masterproject.model.enums.LlmProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GrokLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.GROK);

    public GrokLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://api.x.ai").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GROK;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        try {
            restClient.get()
                    .uri("/v1/models")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "Grok API key is valid.");
        } catch (RestClientResponseException ex) {
            return new LlmHealthResult(false, "Grok check failed: HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            return new LlmHealthResult(false, "Grok check failed: " + ex.getMessage());
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
                throw new IllegalStateException("Grok returned an empty response");
            }
            return content.asText().trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Grok request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Grok request failed: " + ex.getMessage(), ex);
        }
    }
}
