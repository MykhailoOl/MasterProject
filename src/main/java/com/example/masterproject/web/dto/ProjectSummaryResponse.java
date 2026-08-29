package com.example.masterproject.web.dto;

import com.example.masterproject.model.enums.ProjectStatus;
import java.time.Instant;

public class ProjectSummaryResponse {

    private Long id;
    private String title;
    private String initialIdea;
    private ProjectStatus status;
    private String ownerEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private String updatedAtLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInitialIdea() {
        return initialIdea;
    }

    public void setInitialIdea(String initialIdea) {
        this.initialIdea = initialIdea;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedAtLabel() {
        return updatedAtLabel;
    }

    public void setUpdatedAtLabel(String updatedAtLabel) {
        this.updatedAtLabel = updatedAtLabel;
    }
}
