package com.example.masterproject.llm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

class LlmFailureMessagesTests {

    @Test
    void creditAndLicenseFailuresStayOutOfRetryFallback() {
        RestClientResponseException error = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                "{\"error\":\"Your newly created team doesn't have any credits or licenses yet.\"}".getBytes(UTF_8),
                UTF_8);

        assertThat(LlmFailureMessages.forHttp("Grok", error))
                .isEqualTo("Grok could not generate a response because this account has no available credits.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
        assertThat(LlmFailureMessages.isCreditOrLicense(error)).isTrue();
    }

    @Test
    void overloadedModelsAreTemporaryAndCanFallBack() {
        RestClientResponseException error = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unavailable",
                HttpHeaders.EMPTY,
                "{\"error\":{\"message\":\"This model is currently experiencing high demand.\"}}".getBytes(UTF_8),
                UTF_8);

        assertThat(LlmFailureMessages.forHttp("Gemini", error))
                .isEqualTo("Gemini is temporarily unavailable. Please try again.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isTrue();
    }
}
