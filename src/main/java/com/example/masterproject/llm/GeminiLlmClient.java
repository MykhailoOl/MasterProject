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
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GeminiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.GEMINI);

    public GeminiLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GEMINI;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        try {
            String uri = UriComponentsBuilder.fromPath("/v1beta/models")
                    .queryParam("key", apiKey)
                    .toUriString();
            restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();
            return new LlmHealthResult(true, "Gemini API key is valid.");
        } catch (RestClientResponseException ex) {
            return new LlmHealthResult(false, "Gemini check failed: HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            return new LlmHealthResult(false, "Gemini check failed: " + ex.getMessage());
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);

        ArrayNode systemParts = body.putObject("systemInstruction").putArray("parts");
        systemParts.addObject().put("text", systemPrompt);

        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userPrompt);

        String uri = UriComponentsBuilder
                .fromPath("/v1beta/models/" + settings.model() + ":generateContent")
                .queryParam("key", apiKey)
                .toUriString();

        try {
            String response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalStateException("Gemini returned an empty response");
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    text.append(part.get("text").asText());
                }
            }
            if (text.isEmpty()) {
                throw new IllegalStateException("Gemini returned no text content");
            }
            return text.toString().trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Gemini request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Gemini request failed: " + ex.getMessage(), ex);
        }
    }
}
