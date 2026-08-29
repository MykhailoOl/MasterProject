package com.example.masterproject.web.dto;

import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.model.enums.RequirementCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class CreateProjectRequest {

    @NotBlank(message = "{project.initialIdea.notBlank}")
    @Size(min = 10, max = 5000, message = "{project.initialIdea.size}")
    private String initialIdea;

    @NotNull(message = "{project.llmProvider.notNull}")
    private LlmProvider llmProvider;

    private boolean simplifyModeEnabled;

    private List<RequirementCategory> optionalCategories = new ArrayList<>();

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

    public List<RequirementCategory> getOptionalCategories() {
        return optionalCategories;
    }

    public void setOptionalCategories(List<RequirementCategory> optionalCategories) {
        this.optionalCategories = optionalCategories == null ? new ArrayList<>() : optionalCategories;
    }
}
