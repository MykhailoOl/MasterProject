package com.example.masterproject.llm;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class PinnedModelTests {
    @ParameterizedTest
    @EnumSource(LlmProvider.class)
    void strictStudyModeDoesNotChangeModelAfterProviderFailure(LlmProvider provider) {
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
        var expected = server.expect(requestTo(url));
        if (provider != LlmProvider.GEMINI) expected.andExpect(jsonPath("$.model").value("pinned-model"));
        expected.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.complete("test-key", "System", "Stakeholder notes", 0.2, 1200))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }
}
