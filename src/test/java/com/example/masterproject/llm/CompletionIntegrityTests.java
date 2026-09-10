package com.example.masterproject.llm;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class CompletionIntegrityTests {
    @org.junit.jupiter.api.Test
    void developmentFallbackRetainsEachModelsActualSamplingPolicyAndFailure() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new OpenAiLlmClient(builder, new ObjectMapper(), mock(AppLog.class));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("gpt-5-mini"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"model_not_found\"}}"));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.temperature").value(0.2))
                .andRespond(withSuccess("{\"model\":\"gpt-4.1-mini\",\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Valid response\"}}]}", MediaType.APPLICATION_JSON));
        try (var trace = LlmCallTrace.start()) {
            client.complete("test-key", "System", "Text", 0.2, 1200);
            assertThat(trace.attempts()).hasSize(2);
            assertThat(trace.attempts().getFirst()).containsEntry("sentTemperature", null).containsEntry("failureKind", "HTTP_404");
            assertThat(trace.attempts().getLast()).containsEntry("sentTemperature", 0.2).containsEntry("actualModel", "gpt-4.1-mini");
        }
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gpt-5-mini", "gpt-4.1-mini"})
    void openAiSendsSupportedTemperatureAndRecordsOmissionForUnsupportedModel(String model) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new OpenAiLlmClient(builder, new ObjectMapper(), mock(AppLog.class));
        ReflectionTestUtils.setField(client, "modelOverride", model);
        var expectation = server.expect(requestTo("https://api.openai.com/v1/chat/completions"));
        if (model.startsWith("gpt-4")) expectation.andExpect(jsonPath("$.temperature").value(0.2));
        else expectation.andExpect(jsonPath("$.temperature").doesNotExist());
        expectation.andRespond(withSuccess("{\"model\":\"" + model + "\",\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Valid response\"}}]}", MediaType.APPLICATION_JSON));
        try (var trace = LlmCallTrace.start()) {
            assertThat(client.complete("test-key", "System", "Text", 0.2, 1200)).isEqualTo("Valid response");
            var attempt = trace.attempts().getFirst();
            assertThat(attempt.get("actualModel")).isEqualTo(model);
            assertThat(attempt.get("sentTemperature")).isEqualTo(model.startsWith("gpt-4") ? 0.2 : null);
            assertThat(attempt.get("temperaturePolicy")).isEqualTo(model.startsWith("gpt-4") ? "sent" : "provider_default_no_override");
        }
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(LlmProvider.class)
    void tokenLimitedResponsesAreRejectedEvenWhenTheirContentIsWellFormedJson(LlmProvider provider) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var mapper = new ObjectMapper();
        var log = mock(AppLog.class);
        LlmClient client = switch (provider) {
            case OPENAI -> new OpenAiLlmClient(builder, mapper, log);
            case ANTHROPIC -> new AnthropicLlmClient(builder, mapper, log);
            case GEMINI -> new GeminiLlmClient(builder, mapper, log);
            case GROK -> new GrokLlmClient(builder, mapper, log);
        };
        ReflectionTestUtils.setField(client, "modelOverride", "pinned-model");
        ReflectionTestUtils.setField(client, "strictModel", true);
        String url = switch (provider) {
            case OPENAI -> "https://api.openai.com/v1/chat/completions";
            case ANTHROPIC -> "https://api.anthropic.com/v1/messages";
            case GEMINI -> "https://generativelanguage.googleapis.com/v1beta/models/pinned-model:generateContent";
            case GROK -> "https://api.x.ai/v1/chat/completions";
        };
        String response = switch (provider) {
            case OPENAI, GROK -> "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}";
            case ANTHROPIC -> "{\"stop_reason\":\"max_tokens\",\"content\":[{\"type\":\"text\",\"text\":\"{}\"}]}";
            case GEMINI -> "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}";
        };
        server.expect(requestTo(url)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.complete("test-key", "System", "Text", 0.0, 100))
                .hasMessageContaining("incomplete response");
        server.verify();
    }
}
