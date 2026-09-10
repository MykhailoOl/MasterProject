package com.example.masterproject.logging;

import static org.assertj.core.api.Assertions.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AppLogTests {
    @Test
    void diagnosticsRetainStackAndCauseWhileRedactingProviderPayloadsAndCredentials() {
        Logger logger = (Logger) LoggerFactory.getLogger("app.audit");
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captured = new ListAppender<>();
        captured.start(); logger.addAppender(captured);
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("x-request-id", "request-123"); headers.set("retry-after", "60");
            var http = new org.springframework.web.client.RestClientResponseException("raw payload must not escape", 429,
                    "Too Many Requests", headers,
                    "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"private stakeholder text and secret-provider-body\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.charset.StandardCharsets.UTF_8);
            new AppLog().error("INTERVIEW\nFAKE", "Failed\r\nforged-success", new IllegalStateException("Provider unavailable; api_key=private-key", http));
            assertThat(captured.list).hasSize(1);
            assertThat(captured.list.getFirst().getFormattedMessage()).doesNotContain("\n", "\r", "secret-provider-body")
                    .contains("Failed forged-success");
            String stack = ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(captured.list.getFirst().getThrowableProxy());
            assertThat(stack).contains("IllegalStateException", "Provider unavailable", "AppLogTests.java", "Caused by:",
                    "RESOURCE_EXHAUSTED", "status=429", "request-123", "retry-after=60", "[redacted]")
                    .doesNotContain("private-key", "raw payload", "secret-provider-body", "private stakeholder text");
        } finally { logger.detachAppender(captured); captured.stop(); }
    }

    @Test
    void structuredAndBearerCredentialsAreRedactedFromOrdinaryMessages() {
        String safe = DiagnosticSanitizer.message("Authorization: Bearer sk-secret-value api_key=custom-key \"password\":\"private-password\"");
        assertThat(safe).doesNotContain("sk-secret-value", "custom-key", "private-password").contains("[redacted]");
    }
}
