package com.example.masterproject.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.service.ProjectAccessDeniedException;
import com.example.masterproject.service.ProjectNotFoundException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiValidationExceptionHandlerTests {

    private final ApiValidationExceptionHandler handler =
            new ApiValidationExceptionHandler(mock(AppLog.class));

    @Test
    void missingProjectReturnsNotFound() {
        ResponseEntity<Map<String, Object>> response =
                handler.projectNotFound(new ProjectNotFoundException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Project not found.");
    }

    @Test
    void inaccessibleProjectReturnsForbidden() {
        ResponseEntity<Map<String, Object>> response =
                handler.projectAccessDenied(new ProjectAccessDeniedException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("error", "Access to this project is denied.");
    }

    @Test
    void invalidProjectStateReturnsConflict() {
        ResponseEntity<Map<String, Object>> response =
                handler.projectConflict(new IllegalStateException("No configured key"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("error", "The project request could not be completed.");
    }
}
