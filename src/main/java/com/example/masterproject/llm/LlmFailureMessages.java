package com.example.masterproject.llm;

import org.springframework.web.client.RestClientResponseException;

final class LlmFailureMessages {

    private LlmFailureMessages() {
    }

    static String forHttp(String provider, RestClientResponseException error) {
        int status = error.getStatusCode().value();
        String body = body(error);
        if (status == 401) {
            return provider + " API key was rejected.";
        }
        if (status == 402 || isCreditOrLicense(body)) {
            return provider + " could not generate a response because this account has no available credits.";
        }
        if (status == 429) {
            return provider + " is rate limited. Please try again.";
        }
        if (status == 408 || status >= 500) {
            return provider + " is temporarily unavailable. Please try again.";
        }
        return provider + " could not generate a response. Please try again.";
    }

    static boolean isCreditOrLicense(RestClientResponseException error) {
        return error.getStatusCode().value() == 402 || isCreditOrLicense(body(error));
    }

    static boolean canFallbackModel(RestClientResponseException error) {
        int status = error.getStatusCode().value();
        if (status == 401 || status == 402 || isCreditOrLicense(error)) {
            return false;
        }
        String body = body(error);
        return status == 404
                || status == 408
                || status == 429
                || status >= 500
                || body.contains("not found")
                || body.contains("unknown model")
                || body.contains("model_not_found")
                || body.contains("not available");
    }

    private static boolean isCreditOrLicense(String body) {
        return containsAny(
                body,
                "credit",
                "license",
                "billing",
                "quota",
                "insufficient",
                "spend limit",
                "payment required",
                "no available");
    }

    private static boolean containsAny(String body, String... tokens) {
        for (String token : tokens) {
            if (body.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String body(RestClientResponseException error) {
        String body = error.getResponseBodyAsString();
        return body == null ? "" : body.toLowerCase();
    }
}
