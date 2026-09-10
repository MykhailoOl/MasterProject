package com.example.masterproject.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DiagnosticSanitizer {
    private DiagnosticSanitizer() {}

    public static String message(String value) {
        if (value == null) return "";
        String result = value.replaceAll("(?i)(bearer\\s+)[^\\s,;\\\"]+", "$1[redacted]")
                .replaceAll("(?i)((?:api[_-]?key|authorization|password|token|secret)[\\\"']?\\s*[=:]\\s*[\\\"']?)[^\\s,;\\\"']+", "$1[redacted]")
                .replaceAll("\\b(?:sk-[A-Za-z0-9_-]+|AIza[A-Za-z0-9_-]+|xai-[A-Za-z0-9_-]+)\\b", "[redacted]")
                .replaceAll("[\\p{Cntrl}]+", " ");
        return result.length() > 1000 ? result.substring(0, 1000) + " [truncated]" : result;
    }

    public static String http(RestClientResponseException error) {
        StringBuilder details = new StringBuilder("status=").append(error.getStatusCode().value());
        try {
            JsonNode root = new ObjectMapper().readTree(error.getResponseBodyAsString());
            JsonNode node = root.path("error");
            for (String field : new String[]{"code", "status", "type"}) {
                String value = node.path(field).asText("");
                if (value.matches("[A-Za-z0-9_.-]{1,100}")) details.append(" ").append(field).append("=").append(value);
            }
        } catch (RuntimeException ignored) {
        }
        if (error.getResponseHeaders() != null) {
            for (String header : new String[]{"x-request-id", "request-id", "retry-after"}) {
                String value = error.getResponseHeaders().getFirst(header);
                if (value != null && value.matches("[A-Za-z0-9 .,:_-]{1,100}")) details.append(" ").append(header).append("=").append(value);
            }
        }
        return details.toString();
    }

    public static Throwable throwable(Throwable error) {
        return copy(error, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private static Throwable copy(Throwable error, Set<Throwable> seen, int depth) {
        if (error == null || depth >= 8 || !seen.add(error)) return null;
        String detail = error instanceof RestClientResponseException http ? http(http) : message(error.getMessage());
        if (error.getClass().getName().startsWith("tools.jackson.") || error.getClass().getName().startsWith("com.fasterxml.jackson.")) {
            detail = "JSON parsing or mapping failed; payload omitted";
        }
        RuntimeException sanitized = new RuntimeException(error.getClass().getName() + ": " + detail,
                copy(error.getCause(), seen, depth + 1));
        StackTraceElement[] frames = error.getStackTrace();
        sanitized.setStackTrace(java.util.Arrays.copyOf(frames, Math.min(frames.length, 40)));
        return sanitized;
    }
}
