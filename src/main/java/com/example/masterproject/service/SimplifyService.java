package com.example.masterproject.service;

import com.example.masterproject.llm.LlmRuntimeSettings;
import com.example.masterproject.model.entity.Project;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimplifyService {

    private static final Pattern SENTENCE_END = Pattern.compile("[.!?…][\"')\\]]*\\s*$");

    private final ProjectService projectService;
    private final LlmCredentialService llmCredentialService;

    public SimplifyService(ProjectService projectService, LlmCredentialService llmCredentialService) {
        this.projectService = projectService;
        this.llmCredentialService = llmCredentialService;
    }

    @Transactional(readOnly = true)
    public String simplifySelection(Long projectId, String selectedText) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        if (!project.isSimplifyModeEnabled()) {
            throw new IllegalStateException("Simplify mode is disabled for this project.");
        }
        validateSelection(selectedText);

        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                You rewrite text so a complete beginner can understand it.
                Use very simple everyday words.
                Keep the same meaning.
                Do not add new facts.
                Do not ask questions back.
                Output only the rewritten text.
                Make it so simple that nobody needs a second explanation.
                """;
        String userPrompt = "Rewrite this in the simplest possible language:\n\n" + selectedText.trim();

        return llmCredentialService.complete(
                project.getLlmProvider(),
                systemPrompt,
                userPrompt,
                settings.simplifyTemperature(),
                settings.simplifyMaxTokens());
    }

    public void validateSelection(String selectedText) {
        if (selectedText == null || selectedText.isBlank()) {
            throw new IllegalArgumentException("Select at least one full sentence.");
        }
        String trimmed = selectedText.trim();
        if (trimmed.length() < 8) {
            throw new IllegalArgumentException("Selection is too short. Select at least one full sentence.");
        }
        boolean hasSentenceEnd = SENTENCE_END.matcher(trimmed).find()
                || trimmed.indexOf('.') > 0
                || trimmed.indexOf('!') > 0
                || trimmed.indexOf('?') > 0;
        boolean looksLikeFullLine = trimmed.split("\\s+").length >= 4;
        if (!hasSentenceEnd && !looksLikeFullLine) {
            throw new IllegalArgumentException("Select at least one full sentence.");
        }
    }
}
