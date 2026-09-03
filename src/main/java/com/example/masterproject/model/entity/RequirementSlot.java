package com.example.masterproject.model.entity;

import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.enums.RequirementSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "requirement_slots")
public class RequirementSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementCategory category;

    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "assessment_json", columnDefinition = "TEXT")
    private String assessmentJson;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(nullable = false)
    private double completeness = 0.0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementSource source = RequirementSource.USER;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public RequirementCategory getCategory() {
        return category;
    }

    public void setCategory(RequirementCategory category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAssessmentJson() {
        return assessmentJson;
    }

    public void setAssessmentJson(String assessmentJson) {
        this.assessmentJson = assessmentJson;
    }

    public double getCompleteness() {
        return completeness;
    }

    public void setCompleteness(double completeness) {
        this.completeness = completeness;
    }

    public RequirementSource getSource() {
        return source;
    }

    public void setSource(RequirementSource source) {
        this.source = source;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
