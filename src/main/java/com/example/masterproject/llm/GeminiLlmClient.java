package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
            listGenerateContentModels(apiKey);
            return new LlmHealthResult(true, "Gemini API key is valid.");
        } catch (RestClientResponseException ex) {
            appLog.error("LLM", LlmErrorDetails.http("Gemini", "API key check", "GET /v1beta/models", ex));
            return new LlmHealthResult(false, "Gemini API key check failed.");
        } catch (Exception ex) {
            appLog.error("LLM", LlmErrorDetails.unexpected("Gemini", "API key check", "GET /v1beta/models", ex));
            return new LlmHealthResult(false, "Gemini API key check failed.");
        }
    }

    @Override
    public String complete(String apiKey, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        String model = settings.model();
        RestClientResponseException last = null;
        try {
            for (String candidate : models()) {
                model = candidate;
                try {
                    return generateContent(
                            apiKey, candidate, buildBody(systemPrompt, userPrompt, temperature, maxTokens, candidate));
                } catch (RestClientResponseException ex) {
                    last = ex;
                    if (!LlmFailureMessages.canFallbackModel(ex)) {
                        throw completionFailure(candidate, ex);
                    }
                    appLog.warn(
                            "LLM",
                            LlmErrorDetails.http(
                                    "Gemini",
                                    "completion request for model " + candidate,
                                    "POST /v1beta/models/" + candidate + ":generateContent",
                                    ex)
                                    + " | action=trying another Gemini model");
                }
            }
            String listed = resolveWorkingModel(apiKey);
            model = listed;
            if (models().stream().noneMatch(item -> item.equalsIgnoreCase(listed))) {
                return generateContent(apiKey, listed, buildBody(systemPrompt, userPrompt, temperature, maxTokens, listed));
            }
            throw completionFailure(model, Objects.requireNonNull(last));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw completionFailure(model, ex);
        } catch (Exception ex) {
            appLog.error(
                    "LLM",
                    LlmErrorDetails.unexpected(
                            "Gemini",
                            "completion request for model " + model,
                            "POST /v1beta/models/" + model + ":generateContent",
                            ex));
            throw new IllegalStateException("Gemini could not generate a response. Please try again.", ex);
        }
    }

    private List<String> models() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        models.add(settings.model());
        models.addAll(PREFERRED_MODELS);
        return new ArrayList<>(models);
    }

    private IllegalStateException completionFailure(String model, RestClientResponseException error) {
        appLog.error(
                "LLM",
                LlmErrorDetails.http(
                        "Gemini",
                        "completion request for model " + model,
                        "POST /v1beta/models/" + model + ":generateContent",
                        error));
        return new IllegalStateException(LlmFailureMessages.forHttp("Gemini", error), error);
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

}
