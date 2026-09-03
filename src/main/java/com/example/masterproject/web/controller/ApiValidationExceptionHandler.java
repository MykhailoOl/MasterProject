package com.example.masterproject.web.controller;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.service.ProjectAccessDeniedException;
import com.example.masterproject.service.ProjectNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class ApiValidationExceptionHandler {

    private final AppLog appLog;

    public ApiValidationExceptionHandler(AppLog appLog) {
        this.appLog = appLog;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> projectNotFound(ProjectNotFoundException exception) {
        appLog.warn("PROJECT", "Project API request failed: " + exception.getMessage());
        return error(HttpStatus.NOT_FOUND, "Project not found.");
    }

    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> projectAccessDenied(ProjectAccessDeniedException exception) {
        appLog.warn("PROJECT", "Project API access denied: " + exception.getMessage());
        return error(HttpStatus.FORBIDDEN, "Access to this project is denied.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> projectConflict(IllegalStateException exception) {
        appLog.error("PROJECT", "Project API request could not be completed", exception);
        return error(HttpStatus.CONFLICT, "The project request could not be completed.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
