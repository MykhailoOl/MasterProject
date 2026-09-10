package com.example.masterproject.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_revisions", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "revision"}))
public class InterviewRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "revision", nullable = false)
    private long revision;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "document_json", nullable = false, columnDefinition = "TEXT")
    private String documentJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long value) { this.projectId = value; }
    public long getRevision() { return revision; }
    public void setRevision(long value) { this.revision = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public String getDocumentJson() { return documentJson; }
    public void setDocumentJson(String value) { this.documentJson = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
}

