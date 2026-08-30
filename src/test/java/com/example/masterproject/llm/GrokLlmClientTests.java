package com.example.masterproject.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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
}
