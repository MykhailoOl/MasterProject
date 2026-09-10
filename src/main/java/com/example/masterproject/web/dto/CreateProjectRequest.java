package com.example.masterproject.web.dto;

import com.example.masterproject.model.enums.LlmProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest {

    private com.example.masterproject.model.enums.StudyCondition studyCondition;

    public com.example.masterproject.model.enums.StudyCondition getStudyCondition() { return studyCondition; }
    public void setStudyCondition(com.example.masterproject.model.enums.StudyCondition value) { studyCondition = value; }

    @NotBlank(message = "{project.initialIdea.notBlank}")
    @Size(min = 10, max = 5000, message = "{project.initialIdea.size}")
    private String initialIdea;

    @NotNull(message = "{project.llmProvider.notNull}")
    private LlmProvider llmProvider;

    private boolean simplifyModeEnabled;


    public String getInitialIdea() {
        return initialIdea;
    }

    public void setInitialIdea(String initialIdea) {
        this.initialIdea = initialIdea == null ? null : initialIdea.trim();
    }

    public LlmProvider getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public boolean isSimplifyModeEnabled() {
        return simplifyModeEnabled;
    }

    public void setSimplifyModeEnabled(boolean simplifyModeEnabled) {
        this.simplifyModeEnabled = simplifyModeEnabled;
    }

}
