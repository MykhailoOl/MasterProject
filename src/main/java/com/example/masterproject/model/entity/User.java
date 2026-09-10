package com.example.masterproject.model.entity;

import com.example.masterproject.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Column(name = "assignment_method", nullable = false, length = 32)
    private String assignmentMethod = "UNASSIGNED";
    public String getAssignmentMethod() { return assignmentMethod; }
    public void setAssignmentMethod(String value) { assignmentMethod = value; }

    @Enumerated(EnumType.STRING)
    @Column(name = "study_condition", length = 32)
    private com.example.masterproject.model.enums.StudyCondition studyCondition;

    public com.example.masterproject.model.enums.StudyCondition getStudyCondition() { return studyCondition; }
    public void setStudyCondition(com.example.masterproject.model.enums.StudyCondition value) { studyCondition = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(min = 3, max = 24)
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,23}$")
    @Column(nullable = false, length = 24)
    private String username;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
