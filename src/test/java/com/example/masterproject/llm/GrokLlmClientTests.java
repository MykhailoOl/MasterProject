package com.example.masterproject.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.example.masterproject.logging.AppLog;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GrokLlmClientTests {

    @Test
    void healthCheckUsesApiKeyEndpointWithoutGeneratingAResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokLlmClient client = new GrokLlmClient(builder, new ObjectMapper(), mock(AppLog.class));

        server.expect(requestTo("https://api.x.ai/v1/api-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess());

        LlmHealthResult result = client.checkHealth("test-key");

        assertThat(result.ok()).isTrue();
        assertThat(result.message()).isEqualTo("Grok API key is valid.");
        server.verify();
    }

    @Test
    void healthCheckKeepsHttpDetailsOutOfUserMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AppLog appLog = mock(AppLog.class);
        GrokLlmClient client = new GrokLlmClient(builder, new ObjectMapper(), appLog);

        server.expect(requestTo("https://api.x.ai/v1/api-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Invalid API key\"}"));

        LlmHealthResult result = client.checkHealth("test-key");

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).isEqualTo("Grok API key check failed.");
        assertThat(result.message()).doesNotContain("401", "Unauthorized", "Invalid API key");
        verify(appLog).error(eq("LLM"), contains("status=401 Unauthorized"));
        verify(appLog).error(eq("LLM"), contains("response={\"error\":\"Invalid API key\"}"));
        server.verify();
    }

    @Test
    void completeUsesChatCompletionsWithoutTheResponsesApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokLlmClient client = new GrokLlmClient(builder, new ObjectMapper(), mock(AppLog.class));

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("grok-4.6"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"Extracted requirements\"}}]}",
                        MediaType.APPLICATION_JSON));

        String result = client.complete("test-key", "System", "User", 0.3, 2000);

        assertThat(result).isEqualTo("Extracted requirements");
        server.verify();
    }

    @Test
    void completeReportsMissingCreditsWithoutRetryingOtherModels() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AppLog appLog = mock(AppLog.class);
        GrokLlmClient client = new GrokLlmClient(builder, new ObjectMapper(), appLog);

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"permission-denied\",\"error\":\"Your newly created team doesn't have any credits or licenses yet.\"}"));

        assertThatThrownBy(() -> client.complete("test-key", "System", "User", 0.3, 2000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Grok could not generate a response because this account has no available credits.")
                .hasMessageNotContaining("403")
                .hasMessageNotContaining("permission-denied");
        verify(appLog).error(eq("LLM"), contains("status=403 Forbidden"));
        server.verify();
    }

    @Test
    void completeFallsBackToTheNextModelWhenThePreferredModelIsOverloaded() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokLlmClient client = new GrokLlmClient(builder, new ObjectMapper(), mock(AppLog.class));

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("grok-4.6"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"high demand\"}"));
        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("grok-4.5"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"Fallback answer\"}}]}",
                        MediaType.APPLICATION_JSON));

        String result = client.complete("test-key", "System", "User", 0.3, 2000);

        assertThat(result).isEqualTo("Fallback answer");
        server.verify();
    }
}
