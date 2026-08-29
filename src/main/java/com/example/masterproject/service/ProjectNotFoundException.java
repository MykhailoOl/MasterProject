package com.example.masterproject.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
