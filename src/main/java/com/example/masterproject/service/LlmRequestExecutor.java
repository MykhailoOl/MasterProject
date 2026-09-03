package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class LlmRequestExecutor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MILLIS = 500;
    private static final long MAX_DELAY_MILLIS = 5000;

    private final AppLog appLog;

    public LlmRequestExecutor(AppLog appLog) {
        this.appLog = appLog;
    }

    public String execute(LlmProvider provider, Supplier<String> request) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                long delay = retryDelayMillis(ex, attempt);
                if (attempt == MAX_ATTEMPTS || delay < 0) {
                    throw ex;
                }
                appLog.warn(
                        "LLM",
                        provider + " completion temporarily failed. Retrying attempt "
                                + (attempt + 1) + " of " + MAX_ATTEMPTS + ".");
                pause(delay);
            }
        }
        throw lastFailure;
    }

    long retryDelayMillis(Throwable failure, int attempt) {
        RestClientResponseException response = findCause(failure, RestClientResponseException.class);
        if (response != null) {
            int status = response.getStatusCode().value();
            if (status == 429) {
                return retryAfterMillis(response);
            }
            if (status >= 500 || status == 408) {
                long retryAfter = retryAfterMillis(response);
                if (retryAfter >= 0) {
                    return retryAfter;
                }
                return Math.min(MAX_DELAY_MILLIS, BASE_DELAY_MILLIS * (1L << Math.max(0, attempt - 1)));
            }
            return -1;
        }
        return findCause(failure, ResourceAccessException.class) == null
                ? -1
                : Math.min(MAX_DELAY_MILLIS, BASE_DELAY_MILLIS * (1L << Math.max(0, attempt - 1)));
    }

    void pause(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM retry was interrupted.", ex);
        }
    }

    private long retryAfterMillis(RestClientResponseException response) {
        HttpHeaders headers = response.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            long millis = Duration.ofSeconds(Long.parseLong(value.trim())).toMillis();
            return millis <= MAX_DELAY_MILLIS ? millis : -1;
        } catch (Exception ignored) {
        }
        try {
            long millis = Math.max(
                    0,
                    Duration.between(
                                    ZonedDateTime.now(),
                                    ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME))
                            .toMillis());
            return millis <= MAX_DELAY_MILLIS ? millis : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private <T extends Throwable> T findCause(Throwable failure, Class<T> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
