package com.example.masterproject.llm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
    void geminiQuotaExceededIsRateLimitNotCreditsAndDoesNotFallBackToAnotherModel() {
        RestClientResponseException error = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                """
                {"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. Quota exceeded for metric: generate_content_free_tier_requests","status":"RESOURCE_EXHAUSTED"}}
                """
                        .getBytes(UTF_8),
                UTF_8);

        assertThat(LlmFailureMessages.forHttp("Gemini", error))
                .isEqualTo("Gemini is rate limited or over quota. Please try again in a minute.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
        assertThat(LlmFailureMessages.isCreditOrLicense(error)).isFalse();
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

    @Test
    void openaiRateLimitDoesNotFallBackToAnotherModel() {
        RestClientResponseException error = http(
                HttpStatus.TOO_MANY_REQUESTS,
                "{\"error\":{\"message\":\"Rate limit reached for gpt-5-mini\",\"type\":\"tokens\",\"code\":\"rate_limit_exceeded\"}}");

        assertThat(LlmFailureMessages.forHttp("OpenAI", error))
                .isEqualTo("OpenAI is rate limited or over quota. Please try again in a minute.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
    }

    @Test
    void anthropicOverloadedIsTemporaryAndCanFallBack() {
        RestClientResponseException error = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Overloaded",
                HttpHeaders.EMPTY,
                "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}".getBytes(UTF_8),
                UTF_8);

        assertThat(LlmFailureMessages.forHttp("Anthropic", error))
                .isEqualTo("Anthropic is temporarily unavailable. Please try again.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isTrue();
    }

    @Test
    void grokRateLimitDoesNotFallBackToAnotherModel() {
        RestClientResponseException error = http(HttpStatus.TOO_MANY_REQUESTS, "{\"error\":\"Too many requests\"}");

        assertThat(LlmFailureMessages.forHttp("Grok", error))
                .isEqualTo("Grok is rate limited or over quota. Please try again in a minute.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
    }

    @Test
    void rejectedApiKeysStayOutOfModelFallback() {
        RestClientResponseException error = http(HttpStatus.UNAUTHORIZED, "{\"error\":\"invalid_api_key\"}");

        assertThat(LlmFailureMessages.forHttp("OpenAI", error)).isEqualTo("OpenAI API key was rejected.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
    }

    @Test
    void safetyBlocksStayOutOfModelFallback() {
        RestClientResponseException error = http(
                HttpStatus.BAD_REQUEST,
                "{\"error\":{\"message\":\"The response was filtered due to the prompt triggering the safety filter.\"}}");

        assertThat(LlmFailureMessages.forHttp("OpenAI", error))
                .isEqualTo("OpenAI blocked this request. Please rephrase and try again.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isFalse();
    }

    @Test
    void unknownModelsCanFallBack() {
        RestClientResponseException error = http(
                HttpStatus.NOT_FOUND, "{\"error\":{\"message\":\"The model `gpt-5-mini` does not exist\",\"code\":\"model_not_found\"}}");

        assertThat(LlmFailureMessages.forHttp("OpenAI", error))
                .isEqualTo("OpenAI could not generate a response. Please try again.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isTrue();
    }

    @Test
    void anthropicOverload529CanFallBack() {
        RestClientResponseException error = HttpServerErrorException.create(
                HttpStatusCode.valueOf(529),
                "Overloaded",
                HttpHeaders.EMPTY,
                "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}".getBytes(UTF_8),
                UTF_8);

        assertThat(LlmFailureMessages.forHttp("Anthropic", error))
                .isEqualTo("Anthropic is temporarily unavailable. Please try again.");
        assertThat(LlmFailureMessages.canFallbackModel(error)).isTrue();
    }

    private RestClientResponseException http(HttpStatus status, String body) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, body.getBytes(UTF_8), UTF_8);
    }
}
