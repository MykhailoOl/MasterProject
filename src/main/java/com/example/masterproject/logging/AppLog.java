package com.example.masterproject.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("app.audit");

    public void info(String area, String message) { LOGGER.info("{} | {}", clean(area), clean(message)); }
    public void warn(String area, String message) { LOGGER.warn("{} | {}", clean(area), clean(message)); }
    public void error(String area, String message) { LOGGER.error("{} | {}", clean(area), clean(message)); }
    public void error(String area, String message, Throwable error) {
        LOGGER.error("{} | {}", clean(area), clean(message), DiagnosticSanitizer.throwable(error));
    }
    static String clean(String value) {
        if (value == null) return "";
        String cleaned = DiagnosticSanitizer.message(value);
        return cleaned.length() > 2000 ? cleaned.substring(0, 2000) + " [truncated]" : cleaned;
    }
}
