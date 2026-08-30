package com.example.masterproject.logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("app.audit");
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_FILE = Path.of("logs", "app-steps.log");

    public void info(String area, String message) {
        write("INFO", area, message);
    }

    public void warn(String area, String message) {
        write("WARN", area, message);
    }

    public void error(String area, String message) {
        write("ERROR", area, message);
    }

    public void error(String area, String message, Throwable error) {
        String detail = error == null || error.getMessage() == null ? message : message + " (" + error.getMessage() + ")";
        write("ERROR", area, detail);
    }

    private synchronized void write(String level, String area, String message) {
        String line = TIMESTAMP.format(LocalDateTime.now())
                + " | " + level
                + " | " + area
                + " | " + message;
        switch (level) {
            case "WARN" -> LOGGER.warn("{} | {}", area, message);
            case "ERROR" -> LOGGER.error("{} | {}", area, message);
            default -> LOGGER.info("{} | {}", area, message);
        }
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(
                    LOG_FILE,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
}
