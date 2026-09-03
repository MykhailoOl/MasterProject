package com.example.masterproject.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.masterproject.logging.AppLog;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiLlmClientTests {

    @Test
    void completeFallsBackWhenThePreferredModelIsOverloaded() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiLlmClient client = new GeminiLlmClient(builder, new ObjectMapper(), mock(AppLog.class));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":503,\"message\":\"This model is currently experiencing high demand.\",\"status\":\"UNAVAILABLE\"}}"));
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Fallback answer\"}]}}]}",
                        MediaType.APPLICATION_JSON));

        String result = client.complete("test-key", "System", "User", 0.3, 2000);

        assertThat(result).isEqualTo("Fallback answer");
        server.verify();
    }

    @Test
    void completeKeepsHttpDetailsOutOfUserMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiLlmClient client = new GeminiLlmClient(builder, new ObjectMapper(), mock(AppLog.class));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"API key not valid\"}}"));

        assertThatThrownBy(() -> client.complete("test-key", "System", "User", 0.3, 2000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gemini API key was rejected.")
                .hasMessageNotContaining("401")
                .hasMessageNotContaining("API key not valid");
        server.verify();
    }
}
