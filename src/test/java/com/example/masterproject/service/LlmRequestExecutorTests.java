package com.example.masterproject.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.enums.LlmProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class LlmRequestExecutorTests {

    private final NoSleepExecutor executor = new NoSleepExecutor();

    @Test
    void retriesTemporaryServerFailuresWithExponentialBackoff() {
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(LlmProvider.GEMINI, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw wrapped(serverFailure());
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
        assertThat(executor.delays()).containsExactly(500L, 1000L);
    }

    @Test
    void doesNotRetryPermissionFailures() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(LlmProvider.GROK, () -> {
                    attempts.incrementAndGet();
                    throw wrapped(HttpClientErrorException.create(
                            HttpStatus.FORBIDDEN,
                            "Forbidden",
                            HttpHeaders.EMPTY,
                            new byte[0],
                            UTF_8));
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(attempts).hasValue(1);
        assertThat(executor.delays()).isEmpty();
    }

    @Test
    void retriesRateLimitsOnlyWhenProviderSuppliesAShortRetryDelay() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(LlmProvider.ANTHROPIC, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw wrapped(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limited",
                        headers,
                        new byte[0],
                        UTF_8));
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(2);
        assertThat(executor.delays()).containsExactly(1000L);
    }

    @Test
    void doesNotRetryRateLimitWithoutRetryAfter() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(LlmProvider.ANTHROPIC, () -> {
                    attempts.incrementAndGet();
                    throw wrapped(HttpClientErrorException.create(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Spend limit",
                            HttpHeaders.EMPTY,
                            new byte[0],
                            UTF_8));
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void restoresInterruptedStatusWhenRetryWaitIsInterrupted() {
        LlmRequestExecutor realExecutor = new LlmRequestExecutor(mock(AppLog.class));
        try {
            Thread.currentThread().interrupt();

            assertThatThrownBy(() -> realExecutor.pause(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("LLM retry was interrupted.");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private HttpServerErrorException serverFailure() {
        return HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                UTF_8);
    }

    private IllegalStateException wrapped(RuntimeException cause) {
        return new IllegalStateException("Provider failed", cause);
    }

    private static class NoSleepExecutor extends LlmRequestExecutor {

        private final List<Long> delays = new ArrayList<>();

        NoSleepExecutor() {
            super(mock(AppLog.class));
        }

        @Override
        void pause(long delayMillis) {
            delays.add(delayMillis);
        }

        List<Long> delays() {
            return delays;
        }
    }
}
