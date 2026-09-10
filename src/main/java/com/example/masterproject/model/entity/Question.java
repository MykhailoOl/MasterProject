package com.example.masterproject.model.entity;

import com.example.masterproject.model.enums.RequirementCategory;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {
    @Column(name = "generation_origin", nullable = false, length = 32)
    private String generationOrigin = "LOCAL";
    @Column(name = "generation_reason", length = 100)
    private String generationReason;
    @Column(name = "llm_call_id")
    private String llmCallId;
    @Column(name = "prompt_version", length = 100)
    private String promptVersion;
    public String getGenerationOrigin() { return generationOrigin; }
    public void setGenerationOrigin(String value) { generationOrigin = value; }
    public String getGenerationReason() { return generationReason; }
    public void setGenerationReason(String value) { generationReason = value; }
    public String getLlmCallId() { return llmCallId; }
    public void setLlmCallId(String value) { llmCallId = value; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String value) { promptVersion = value; }

    @Column(name = "focus_capability", length = 80)
    private String focusCapability;

    @Column(name = "question_kind", nullable = false, length = 32)
    private String questionKind = "INTERVIEW";

    public String getFocusCapability() { return focusCapability; }
    public void setFocusCapability(String value) { focusCapability = value; }
    public String getQuestionKind() { return questionKind; }
    public void setQuestionKind(String value) { questionKind = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ElicitationSession session;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementCategory category;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Size(max = 2000)
    @Column(name = "simplified_text", columnDefinition = "TEXT")
    private String simplifiedText;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Size(max = 64)
    @Column(name = "focus_criterion", length = 64)
    private String focusCriterion;

    @Column(name = "answer_example", columnDefinition = "TEXT")
    private String answerExample;

    @Min(1)
    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ElicitationSession getSession() {
        return session;
    }

    public void setSession(ElicitationSession session) {
        this.session = session;
    }

    public RequirementCategory getCategory() {
        return category;
    }

    public void setCategory(RequirementCategory category) {
        this.category = category;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getSimplifiedText() {
        return simplifiedText;
    }

    public void setSimplifiedText(String simplifiedText) {
        this.simplifiedText = simplifiedText;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getFocusCriterion() {
        return focusCriterion;
    }

    public void setFocusCriterion(String focusCriterion) {
        this.focusCriterion = focusCriterion;
    }

    public String getAnswerExample() {
        return answerExample;
    }

    public void setAnswerExample(String answerExample) {
        this.answerExample = answerExample;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
