package com.example.masterproject.llm;

import org.springframework.web.client.RestClientResponseException;

final class LlmErrorDetails {

    private LlmErrorDetails() {
    }

    static String http(String provider, String operation, String endpoint, RestClientResponseException error) {
        LlmCallTrace.failure(error);
        return provider + " " + operation + " failed"
                + " | endpoint=" + endpoint
                + " | " + com.example.masterproject.logging.DiagnosticSanitizer.http(error);
    }

    static String unexpected(String provider, String operation, String endpoint, Exception error) {
        LlmCallTrace.failure(error);
        return provider + " " + operation + " failed"
                + " | endpoint=" + endpoint
                + " | exception=" + error.getClass().getName()
                + " | message=" + com.example.masterproject.logging.DiagnosticSanitizer.message(error.getMessage());
    }

}
