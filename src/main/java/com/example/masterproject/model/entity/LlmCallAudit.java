package com.example.masterproject.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "llm_call_audits")
public class LlmCallAudit {
    @Id private String id;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(nullable = false) private String phase;
    @Column(name = "prompt_version", nullable = false) private String promptVersion;
    @Column(nullable = false) private String provider;
    @Column(nullable = false) private String outcome;
    @Column(name = "requested_temperature", nullable = false) private double requestedTemperature;
    @Column(name = "duration_ms", nullable = false) private long durationMs;
    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT") private String metadataJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long value) { projectId = value; }
    public String getPhase() { return phase; }
    public void setPhase(String value) { phase = value; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String value) { promptVersion = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { outcome = value; }
    public double getRequestedTemperature() { return requestedTemperature; }
    public void setRequestedTemperature(double value) { requestedTemperature = value; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long value) { durationMs = value; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String value) { metadataJson = value; }
    public Instant getCreatedAt() { return createdAt; }
}
