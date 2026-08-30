package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import java.net.URI;
import java.util.ArrayList;
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
public class GeminiLlmClient implements LlmClient {

    private static final List<String> PREFERRED_MODELS = List.of(
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash-lite",
            "gemini-flash-latest",
            "gemini-2.5-flash");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppLog appLog;
    private final LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(LlmProvider.GEMINI);

    public GeminiLlmClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, AppLog appLog) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
        this.appLog = appLog;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GEMINI;
    }

    @Override
    public LlmHealthResult checkHealth(String apiKey) {
        try {
            String chosen = resolveWorkingModel(apiKey);
            return new LlmHealthResult(true, "Gemini API key is valid. Using model " + chosen + ".");
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", "Gemini health check failed with HTTP " + ex.getStatusCode().value()
                    + ": " + shortBody(ex));
            return new LlmHealthResult(false, "Gemini check failed: HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            appLog.error("LLM", "Gemini health check failed", ex);
            return new LlmHealthResult(false, "Gemini check failed: " + ex.getMessage());
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        String model = settings.model();
        try {
            try {
                return generateContent(apiKey, model, buildBody(systemPrompt, userPrompt, temperature, maxTokens, model));
            } catch (RestClientResponseException first) {
                if (first.getStatusCode().value() != 404) {
                    throw first;
                }
                appLog.warn("LLM", "Gemini model " + model + " returned HTTP 404. Looking up an available model.");
                model = resolveWorkingModel(apiKey);
                return generateContent(apiKey, model, buildBody(systemPrompt, userPrompt, temperature, maxTokens, model));
            }
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", "Gemini request failed: HTTP " + ex.getStatusCode().value()
                    + " for model " + model + ": " + shortBody(ex));
            throw new IllegalStateException(
                    "Gemini request failed: HTTP " + ex.getStatusCode().value() + " (" + shortBody(ex) + ")", ex);
        } catch (Exception ex) {
            appLog.error("LLM", "Gemini request failed", ex);
            throw new IllegalStateException("Gemini request failed: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode buildBody(
            String systemPrompt, String userPrompt, double temperature, int maxTokens, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", maxTokens);
        if (usesThinkingLevel(model)) {
            generationConfig.putObject("thinkingConfig")
                    .put("thinkingLevel", thinkingLevelFor(temperature));
        } else {
            generationConfig.put("temperature", temperature);
        }

        ArrayNode systemParts = body.putObject("systemInstruction").putArray("parts");
        systemParts.addObject().put("text", systemPrompt);

        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userPrompt);
        return body;
    }

    private boolean usesThinkingLevel(String model) {
        String normalized = stripModelsPrefix(model).toLowerCase();
        return normalized.startsWith("gemini-3") || "gemini-flash-latest".equals(normalized);
    }

    private String generateContent(String apiKey, String model, ObjectNode body) {
        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                + stripModelsPrefix(model) + ":generateContent");
        appLog.info("LLM", "Calling Gemini generateContent with model " + stripModelsPrefix(model) + ".");
        String response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", apiKey)
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
    }

    private String thinkingLevelFor(double temperature) {
        if (temperature < 0.35) {
            return "low";
        }
        if (temperature < 0.7) {
            return "medium";
        }
        return "high";
    }

    private String resolveWorkingModel(String apiKey) {
        List<String> available = listGenerateContentModels(apiKey);
        for (String preferred : PREFERRED_MODELS) {
            if (available.stream().anyMatch(item -> item.equalsIgnoreCase(preferred))) {
                return preferred;
            }
        }
        if (!available.isEmpty()) {
            return available.getFirst();
        }
        return settings.model();
    }

    private List<String> listGenerateContentModels(String apiKey) {
        String response = restClient.get()
                .uri("/v1beta/models")
                .header("x-goog-api-key", apiKey)
                .retrieve()
                .body(String.class);
        JsonNode models = objectMapper.readTree(response).path("models");
        List<String> names = new ArrayList<>();
        if (!models.isArray()) {
            return names;
        }
        for (JsonNode model : models) {
            boolean supportsGenerate = false;
            JsonNode methods = model.path("supportedGenerationMethods");
            if (methods.isArray()) {
                for (JsonNode method : methods) {
                    if ("generateContent".equals(method.asText())) {
                        supportsGenerate = true;
                        break;
                    }
                }
            }
            if (!supportsGenerate) {
                continue;
            }
            String name = stripModelsPrefix(model.path("name").asText(""));
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        names.sort((left, right) -> {
            boolean leftFlash = left.toLowerCase().contains("flash");
            boolean rightFlash = right.toLowerCase().contains("flash");
            if (leftFlash == rightFlash) {
                return left.compareToIgnoreCase(right);
            }
            return leftFlash ? -1 : 1;
        });
        return names;
    }

    private String stripModelsPrefix(String name) {
        if (name == null) {
            return "";
        }
        return name.startsWith("models/") ? name.substring("models/".length()) : name;
    }

    private String shortBody(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "no response body";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }
}
