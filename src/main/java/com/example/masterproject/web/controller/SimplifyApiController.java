package com.example.masterproject.web.controller;

import com.example.masterproject.service.SimplifyService;
import com.example.masterproject.web.dto.SimplifyTextRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class SimplifyApiController {

    private final SimplifyService simplifyService;

    public SimplifyApiController(SimplifyService simplifyService) {
        this.simplifyService = simplifyService;
    }

    @PostMapping("/simplify")
    public ResponseEntity<?> simplify(
            @PathVariable Long projectId, @Valid @RequestBody SimplifyTextRequest request) {
        try {
            String simplified = simplifyService.simplifySelection(projectId, request.getSelectedText());
            return ResponseEntity.ok(Map.of("simplifiedText", simplified));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
