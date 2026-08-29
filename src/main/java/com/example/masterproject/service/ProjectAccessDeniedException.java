package com.example.masterproject.service;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException(Long projectId) {
        super("Access denied for project: " + projectId);
    }
}
