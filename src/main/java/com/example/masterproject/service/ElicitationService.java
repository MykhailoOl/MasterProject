package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.llm.LlmRuntimeSettings;
import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.Question;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.ProjectStatus;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.AnswerRepository;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ProjectCategoryRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.QuestionRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ElicitationService {

    public record ElicitationView(
            Project project,
            Question currentQuestion,
            boolean complete,
            int answeredCount,
            int totalBudget,
            List<String> choices,
            String suggestedAnswer,
            boolean titleChoiceStep,
            boolean overallIdeaStep) {
    }

    private final ProjectService projectService;
    private final ElicitationSessionRepository sessionRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final RequirementSlotRepository requirementSlotRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ProjectRepository projectRepository;
    private final CompletenessSnapshotService completenessSnapshotService;
    private final LlmCredentialService llmCredentialService;
    private final ObjectMapper objectMapper;
    private final AppLog appLog;

    public ElicitationService(
            ProjectService projectService,
            ElicitationSessionRepository sessionRepository,
            ProjectCategoryRepository projectCategoryRepository,
            RequirementSlotRepository requirementSlotRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            ProjectRepository projectRepository,
            CompletenessSnapshotService completenessSnapshotService,
            LlmCredentialService llmCredentialService,
            ObjectMapper objectMapper,
            AppLog appLog) {
        this.projectService = projectService;
        this.sessionRepository = sessionRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.requirementSlotRepository = requirementSlotRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.projectRepository = projectRepository;
        this.completenessSnapshotService = completenessSnapshotService;
        this.llmCredentialService = llmCredentialService;
        this.objectMapper = objectMapper;
        this.appLog = appLog;
    }

    @Transactional
    public ElicitationView getOrAdvance(Long projectId) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        ElicitationSession session = requireSession(project);

        Optional<Question> unanswered = findUnansweredQuestion(session);
        if (unanswered.isPresent()) {
            return buildView(project, session, unanswered.get());
        }

        Optional<ProjectCategory> nextCategory = findNextCategory(project);
        if (nextCategory.isEmpty()) {
            markCompleted(project, session);
            return buildView(project, session, null);
        }

        Question question = generateQuestion(project, session, nextCategory.get());
        appLog.info(
                "ELICITATION",
                "Project #" + project.getId() + " asked a " + question.getCategory() + " question.");
        return buildView(project, session, question);
    }

    @Transactional
    public ElicitationView submitAnswer(Long projectId, Long questionId, String answerText) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        ElicitationSession session = requireSession(project);
        Question question = questionRepository
                .findFirstBySessionAndId(session, questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (answerRepository.existsByQuestion(question)) {
            throw new IllegalStateException("Question already answered");
        }

        String normalized = answerText == null ? "" : answerText.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Answer is required");
        }

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setAnswerText(normalized);
        answer.setAnsweredAt(Instant.now());
        answerRepository.save(answer);

        ProjectCategory projectCategory = projectCategoryRepository
                .findByProjectAndCategory(project, question.getCategory())
                .orElseThrow();
        projectCategory.setQuestionsAsked(projectCategory.getQuestionsAsked() + 1);
        projectCategoryRepository.save(projectCategory);

        if (question.getCategory() == RequirementCategory.PROJECT_TITLE) {
            applyTitleAnswer(project, question, normalized);
        } else if (question.getCategory() == RequirementCategory.OVERALL_IDEA) {
            applyOverallIdeaAnswer(project, question, normalized);
        } else {
            updateSlotFromAnswer(project, question, normalized);
        }

        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
        completenessSnapshotService.captureAfterAnswer(project, session, answer);

        appLog.info(
                "ELICITATION",
                "Project #" + project.getId() + " received an answer for " + question.getCategory() + ".");
        return getOrAdvance(projectId);
    }

    private Question generateQuestion(Project project, ElicitationSession session, ProjectCategory categoryRow) {
        if (categoryRow.getCategory() == RequirementCategory.PROJECT_TITLE) {
            return generateTitleQuestion(project, session, categoryRow);
        }
        if (categoryRow.getCategory() == RequirementCategory.OVERALL_IDEA) {
            return generateOverallIdeaQuestion(project, session, categoryRow);
        }
        return generateStandardQuestion(project, session, categoryRow);
    }

    private Question generateStandardQuestion(
            Project project, ElicitationSession session, ProjectCategory categoryRow) {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(categoryRow.getCategory());
        RequirementSlot slot = requirementSlotRepository
                .findByProjectAndCategory(project, categoryRow.getCategory())
                .orElseThrow();

        String knownContext = buildKnownContext(project);

        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                You help turn a vague software idea into coding-ready requirements.
                Ask exactly one focused clarification question for the given category.
                Return plain text only. Do not number the question. Do not add preamble.
                """;
        String userPrompt = """
                Working title: %s
                Initial idea: %s
                Category to clarify: %s (%s)
                Current notes for this category: %s
                Already known from other categories:
                %s
                Ask one concrete question that fills an important missing detail for this category.
                """.formatted(
                project.getTitle(),
                project.getInitialIdea(),
                definition.displayName(),
                definition.description(),
                slot.getValue() == null || slot.getValue().isBlank() ? "(empty)" : slot.getValue(),
                knownContext.isBlank() ? "(none yet)" : knownContext);

        String questionText = llmCredentialService.complete(
                project.getLlmProvider(),
                systemPrompt,
                userPrompt,
                settings.elicitationTemperature(),
                settings.elicitationMaxTokens());

        return persistQuestion(session, categoryRow.getCategory(), questionText, null);
    }

    private Question generateTitleQuestion(
            Project project, ElicitationSession session, ProjectCategory categoryRow) {
        String knownContext = buildKnownContext(project);
        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                Propose short product titles for a software project.
                Return ONLY compact JSON: {"prompt":"string","choices":["t1","t2","t3","t4"]}.
                choices must contain 3 to 5 distinct title options based on the gathered requirements.
                Titles must be concise (2 to 6 words). Do not include markdown.
                """;
        String userPrompt = """
                Initial idea: %s
                Working title: %s
                Gathered requirements:
                %s
                """.formatted(
                project.getInitialIdea(),
                project.getTitle(),
                knownContext.isBlank() ? "(none)" : knownContext);

        String raw = llmCredentialService.complete(
                project.getLlmProvider(),
                systemPrompt,
                userPrompt,
                settings.elicitationTemperature(),
                settings.elicitationMaxTokens());

        String prompt = "Choose the final project title. Pick one suggestion or write your own.";
        List<String> choices = fallbackTitleChoices(project);
        try {
            JsonNode node = objectMapper.readTree(extractJson(raw));
            if (node.hasNonNull("prompt") && !node.get("prompt").asText().isBlank()) {
                prompt = node.get("prompt").asText().trim();
            }
            if (node.has("choices") && node.get("choices").isArray()) {
                List<String> parsed = new ArrayList<>();
                for (JsonNode choice : node.get("choices")) {
                    String text = choice.asText("").trim();
                    if (!text.isBlank() && parsed.stream().noneMatch(existing -> existing.equalsIgnoreCase(text))) {
                        parsed.add(text);
                    }
                }
                if (parsed.size() >= 2) {
                    choices = parsed;
                }
            }
        } catch (Exception ignored) {
        }

        String optionsJson;
        try {
            optionsJson = objectMapper.writeValueAsString(choices);
        } catch (Exception ex) {
            optionsJson = "[]";
        }
        return persistQuestion(session, categoryRow.getCategory(), prompt, optionsJson);
    }

    private Question generateOverallIdeaQuestion(
            Project project, ElicitationSession session, ProjectCategory categoryRow) {
        String knownContext = buildKnownContext(project);
        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                Write one short overall product description for a coding-oriented SPEC.
                It must stay consistent with the gathered taxonomy notes and the initial idea.
                Do not contradict later requirements. Do not restate category-by-category details.
                Keep it to 2-4 sentences. Return plain text only.
                """;
        String userPrompt = """
                Initial idea: %s
                Final title: %s
                Gathered taxonomy notes:
                %s
                """.formatted(
                project.getInitialIdea(),
                project.getTitle(),
                knownContext.isBlank() ? "(none)" : knownContext);

        String draft = llmCredentialService.complete(
                project.getLlmProvider(),
                systemPrompt,
                userPrompt,
                settings.elicitationTemperature(),
                settings.elicitationMaxTokens());
        if (draft == null || draft.isBlank()) {
            draft = project.getInitialIdea();
        } else {
            draft = draft.trim();
        }

        String prompt = "Review and edit the overall idea so it fits the gathered requirements "
                + "without repeating each taxonomy section.";
        return persistQuestion(session, categoryRow.getCategory(), prompt, null, draft);
    }

    private Question persistQuestion(
            ElicitationSession session, RequirementCategory category, String questionText, String optionsJson) {
        return persistQuestion(session, category, questionText, optionsJson, null);
    }

    private Question persistQuestion(
            ElicitationSession session,
            RequirementCategory category,
            String questionText,
            String optionsJson,
            String suggestedDraft) {
        Question question = new Question();
        question.setSession(session);
        question.setCategory(category);
        question.setQuestionText(questionText);
        question.setOptionsJson(optionsJson);
        question.setSimplifiedText(suggestedDraft);
        question.setQuestionOrder((int) questionRepository.countBySession(session) + 1);
        question.setCreatedAt(Instant.now());
        return questionRepository.save(question);
    }

    private void applyTitleAnswer(Project project, Question question, String answerText) {
        RequirementSlot slot = requirementSlotRepository
                .findByProjectAndCategory(project, RequirementCategory.PROJECT_TITLE)
                .orElseThrow();
        slot.setValue(answerText);
        slot.setCompleteness(1.0);
        slot.setUpdatedAt(Instant.now());
        requirementSlotRepository.save(slot);
        project.setTitle(answerText);
    }

    private void applyOverallIdeaAnswer(Project project, Question question, String answerText) {
        RequirementSlot slot = requirementSlotRepository
                .findByProjectAndCategory(project, RequirementCategory.OVERALL_IDEA)
                .orElseThrow();
        slot.setValue(answerText);
        slot.setCompleteness(1.0);
        slot.setUpdatedAt(Instant.now());
        requirementSlotRepository.save(slot);
    }

    private void updateSlotFromAnswer(Project project, Question question, String answerText) {
        RequirementSlot slot = requirementSlotRepository
                .findByProjectAndCategory(project, question.getCategory())
                .orElseThrow();

        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                You consolidate requirement notes for a software specification.
                Return ONLY compact JSON with keys value (string) and completeness (number 0 to 1).
                value must be a short, actionable summary suitable for a coding agent.
                Score completeness using exactly one of 0, 0.25, 0.5, 0.75, or 1.
                0 means absent or off-topic.
                0.25 means relevant intent is present but remains vague and not actionable.
                0.5 means partially actionable with major decisions or constraints still missing.
                0.75 means mostly actionable with only minor details missing.
                1 means complete, unambiguous, feasible, and verifiable enough that no further
                question is needed for this category.
                Judge only the supplied category and support the score with the consolidated value.
                """;
        String userPrompt = """
                Category: %s
                Category purpose: %s
                Existing notes: %s
                Latest question: %s
                Latest answer: %s
                """.formatted(
                TaxonomyCatalog.require(question.getCategory()).displayName(),
                TaxonomyCatalog.require(question.getCategory()).description(),
                slot.getValue() == null ? "" : slot.getValue(),
                question.getQuestionText(),
                answerText);

        String raw = llmCredentialService.complete(
                project.getLlmProvider(),
                systemPrompt,
                userPrompt,
                0.0,
                settings.elicitationMaxTokens());

        String value = answerText;
        double completeness = rubricScore(Math.max(slot.getCompleteness(), 0.25));
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("value") && !node.get("value").asText().isBlank()) {
                value = node.get("value").asText().trim();
            }
            if (node.has("completeness")) {
                completeness = rubricScore(node.get("completeness").asDouble());
            }
        } catch (Exception ignored) {
            if (slot.getValue() != null && !slot.getValue().isBlank()) {
                value = slot.getValue() + " | " + answerText;
            }
        }

        slot.setValue(value);
        slot.setCompleteness(completeness);
        slot.setUpdatedAt(Instant.now());
        requirementSlotRepository.save(slot);
    }

    private double rubricScore(double score) {
        double bounded = Math.max(0.0, Math.min(1.0, score));
        return Math.round(bounded * 4.0) / 4.0;
    }

    private String buildKnownContext(Project project) {
        return requirementSlotRepository.findByProjectOrderByCategoryAsc(project).stream()
                .filter(item -> item.getValue() != null && !item.getValue().isBlank())
                .filter(item -> !TaxonomyCatalog.isClosing(item.getCategory()))
                .map(item -> TaxonomyCatalog.require(item.getCategory()).displayName() + ": " + item.getValue())
                .collect(Collectors.joining("\n"));
    }

    private List<String> fallbackTitleChoices(Project project) {
        String base = project.getTitle() == null || project.getTitle().isBlank()
                ? "Spec Project"
                : project.getTitle();
        return List.of(base, base + " App", "MVP " + base);
    }

    private Optional<ProjectCategory> findNextCategory(Project project) {
        List<ProjectCategory> categories = projectCategoryRepository.findByProjectOrderByIdAsc(project);
        for (ProjectCategory category : categories) {
            if (category.getQuestionsAsked() >= category.getMaxQuestions()) {
                continue;
            }
            RequirementSlot slot = requirementSlotRepository
                    .findByProjectAndCategory(project, category.getCategory())
                    .orElse(null);
            if (slot != null && slot.getCompleteness() >= 0.95 && category.getQuestionsAsked() > 0) {
                continue;
            }
            return Optional.of(category);
        }
        return Optional.empty();
    }

    private Optional<Question> findUnansweredQuestion(ElicitationSession session) {
        return questionRepository.findBySessionOrderByQuestionOrderAsc(session).stream()
                .filter(question -> !answerRepository.existsByQuestion(question))
                .findFirst();
    }

    private ElicitationSession requireSession(Project project) {
        return sessionRepository
                .findFirstByProjectOrderByStartedAtDesc(project)
                .orElseThrow(() -> new IllegalStateException("Elicitation session missing"));
    }

    private void markCompleted(Project project, ElicitationSession session) {
        if (session.getCompletedAt() == null) {
            session.setCompletedAt(Instant.now());
            sessionRepository.save(session);
        }
        project.setStatus(ProjectStatus.COMPLETED);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
        appLog.info("ELICITATION", "Project #" + project.getId() + " elicitation completed.");
    }

    private ElicitationView buildView(Project project, ElicitationSession session, Question question) {
        List<ProjectCategory> categories = projectCategoryRepository.findByProjectOrderByIdAsc(project);
        int totalBudget = categories.stream().mapToInt(ProjectCategory::getMaxQuestions).sum();
        int answeredCount = categories.stream().mapToInt(ProjectCategory::getQuestionsAsked).sum();
        boolean complete = question == null && findNextCategory(project).isEmpty();
        List<String> choices = List.of();
        String suggestedAnswer = null;
        boolean titleChoiceStep = false;
        boolean overallIdeaStep = false;
        if (question != null) {
            titleChoiceStep = question.getCategory() == RequirementCategory.PROJECT_TITLE;
            overallIdeaStep = question.getCategory() == RequirementCategory.OVERALL_IDEA;
            if (titleChoiceStep) {
                choices = parseChoices(question.getOptionsJson());
            }
            if (overallIdeaStep) {
                suggestedAnswer = question.getSimplifiedText();
            }
        }
        return new ElicitationView(
                project,
                question,
                complete,
                answeredCount,
                totalBudget,
                choices,
                suggestedAnswer,
                titleChoiceStep,
                overallIdeaStep);
    }

    private List<String> parseChoices(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(optionsJson);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> choices = new ArrayList<>();
            for (JsonNode choice : node) {
                String text = choice.asText("").trim();
                if (!text.isBlank()) {
                    choices.add(text);
                }
            }
            return choices;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
