package com.example.masterproject.service;

import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.enums.LlmProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StudyProtocol {
    private final Environment environment;
    public StudyProtocol(Environment environment) { this.environment = environment; }
    public String configuredModel(LlmProvider provider) {
        String expectedProvider = environment.getProperty("app.study.provider", "OPENAI");
        String model = environment.getProperty("app.llm." + provider.name().toLowerCase(java.util.Locale.ROOT) + ".model", "").trim();
        if (!environment.getProperty("app.study.strict-model", Boolean.class, false)
                || !expectedProvider.equals(provider.name()) || model.isBlank()) {
            throw new IllegalStateException("Study setup is incomplete. Ask the researcher to prepare the interview settings.");
        }
        return model;
    }
    public void validate(Project project, ElicitationSession session) {
        if (session.isStudyEnrolled() && (!configuredModel(project.getLlmProvider()).equals(session.getStudyModel())
                || project.isSimplifyModeEnabled())) {
            throw new IllegalStateException("The study model or interview settings changed. Ask the administrator to restore the session's recorded settings.");
        }
    }
}
