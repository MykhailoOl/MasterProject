package com.example.masterproject.llm;

import org.springframework.web.client.RestClientResponseException;

final class LlmErrorDetails {

    private LlmErrorDetails() {
    }

    static String http(String provider, String operation, String endpoint, RestClientResponseException error) {
        return provider + " " + operation + " failed"
                + " | endpoint=" + endpoint
                + " | status=" + error.getStatusCode().value() + " " + error.getStatusText()
                + " | response=" + responseBody(error);
    }

    static String unexpected(String provider, String operation, String endpoint, Exception error) {
        return provider + " " + operation + " failed"
                + " | endpoint=" + endpoint
                + " | exception=" + error.getClass().getName()
                + " | message=" + valueOrNone(error.getMessage());
    }

    private static String responseBody(RestClientResponseException error) {
        String body = error.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "(empty)";
        }
        return body.replaceAll("\\s+", " ").trim();
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "(none)" : value.replaceAll("\\s+", " ").trim();
    }
}
