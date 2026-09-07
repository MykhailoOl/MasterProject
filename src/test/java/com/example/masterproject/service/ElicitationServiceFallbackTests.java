package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.Question;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.enums.StudyCondition;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.AnswerRepository;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ProjectCategoryRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.QuestionRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ElicitationServiceFallbackTests {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ElicitationSessionRepository sessionRepository = mock(ElicitationSessionRepository.class);
    private final ProjectCategoryRepository categoryRepository = mock(ProjectCategoryRepository.class);
    private final RequirementSlotRepository slotRepository = mock(RequirementSlotRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final CompletenessSnapshotService snapshotService = mock(CompletenessSnapshotService.class);
    private final RequirementAssessmentService assessmentService = mock(RequirementAssessmentService.class);
    private final LlmCredentialService llmCredentialService = mock(LlmCredentialService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GuidedElicitationPlanner planner = new GuidedElicitationPlanner(objectMapper);
    private final AppLog appLog = mock(AppLog.class);
    private final ElicitationService service = new ElicitationService(
            projectService,
            sessionRepository,
            categoryRepository,
            slotRepository,
            questionRepository,
            answerRepository,
            projectRepository,
            snapshotService,
            assessmentService,
            planner,
            llmCredentialService,
            objectMapper,
            appLog);
    private Project project;
    private ElicitationSession session;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setTitle("Assignment Tracker");
        project.setInitialIdea("A web application that helps students track assignments.");
        project.setLlmProvider(LlmProvider.GROK);

        session = new ElicitationSession();
        session.setId(2L);
        session.setProject(project);
        session.setConditionTag(StudyCondition.GUIDED);

        when(projectService.getProjectForCurrentUser(1L)).thenReturn(project);
        when(sessionRepository.findFirstByProjectOrderByStartedAtDesc(project)).thenReturn(Optional.of(session));
        when(questionRepository.findBySessionOrderByQuestionOrderAsc(session)).thenReturn(List.of());
        when(questionRepository.countBySession(session)).thenReturn(0L);
        when(questionRepository.save(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(llmCredentialService.complete(
                        any(),
                        anyString(),
                        anyString(),
                        anyDouble(),
                        anyInt()))
                .thenThrow(new IllegalStateException("Provider unavailable"));
    }

    @Test
    void standardQuestionFallsBackToTheResearchedQuestionBank() {
        configureCategory(RequirementCategory.GOAL);

        ElicitationService.ElicitationView view = service.getOrAdvance(1L);

        assertThat(view.currentQuestion().getQuestionText())
                .isEqualTo("What concrete problem or frustration should the first version remove for people?");
        assertThat(view.currentQuestion().getFocusCriterion()).isEqualTo("problem");
        assertThat(view.answerExample()).contains("Assignment Tracker");
        assertThat(view.answerExample()).containsIgnoringCase("problem");
    }

    @Test
    void titleStepFallsBackToLocalTitleChoices() {
        configureCategory(RequirementCategory.PROJECT_TITLE);

        ElicitationService.ElicitationView view = service.getOrAdvance(1L);

        assertThat(view.titleChoiceStep()).isTrue();
        assertThat(view.choices())
                .containsExactly("Assignment Tracker", "Assignment Tracker App", "MVP Assignment Tracker");
    }

    @Test
    void overallIdeaStepFallsBackToTheInitialIdea() {
        configureCategory(RequirementCategory.OVERALL_IDEA);

        ElicitationService.ElicitationView view = service.getOrAdvance(1L);

        assertThat(view.overallIdeaStep()).isTrue();
        assertThat(view.suggestedAnswer()).isEqualTo(project.getInitialIdea());
    }

    @Test
    void fastFinishWalksSequentialTopicsUntilComplete() {
        TaxonomyCatalog.Definition goalDefinition = TaxonomyCatalog.require(RequirementCategory.GOAL);
        ProjectCategory goal = categoryRow(RequirementCategory.GOAL, goalDefinition);
        ProjectCategory title = categoryRow(
                RequirementCategory.PROJECT_TITLE, TaxonomyCatalog.require(RequirementCategory.PROJECT_TITLE));
        RequirementSlot goalSlot = slot(RequirementCategory.GOAL);
        RequirementSlot titleSlot = slot(RequirementCategory.PROJECT_TITLE);
        List<Question> storedQuestions = new ArrayList<>();
        Map<Long, Answer> answers = new HashMap<>();
        AtomicLong ids = new AtomicLong(1);

        when(categoryRepository.findByProjectOrderByIdAsc(project)).thenReturn(List.of(goal, title));
        when(categoryRepository.findByProjectAndCategory(project, RequirementCategory.GOAL))
                .thenReturn(Optional.of(goal));
        when(categoryRepository.findByProjectAndCategory(project, RequirementCategory.PROJECT_TITLE))
                .thenReturn(Optional.of(title));
        when(slotRepository.findByProjectOrderByCategoryAsc(project)).thenReturn(List.of(goalSlot, titleSlot));
        when(slotRepository.findByProjectAndCategory(project, RequirementCategory.GOAL))
                .thenReturn(Optional.of(goalSlot));
        when(slotRepository.findByProjectAndCategory(project, RequirementCategory.PROJECT_TITLE))
                .thenReturn(Optional.of(titleSlot));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            if (question.getId() == null) {
                question.setId(ids.getAndIncrement());
            }
            storedQuestions.removeIf(existing -> existing.getId().equals(question.getId()));
            storedQuestions.add(question);
            return question;
        });
        when(questionRepository.findBySessionOrderByQuestionOrderAsc(session))
                .thenAnswer(invocation -> List.copyOf(storedQuestions));
        when(questionRepository.countBySession(session)).thenAnswer(invocation -> (long) storedQuestions.size());
        when(questionRepository.findFirstBySessionAndId(eq(session), anyLong()))
                .thenAnswer(invocation -> storedQuestions.stream()
                        .filter(question -> question.getId().equals(invocation.getArgument(1)))
                        .findFirst());
        when(answerRepository.existsByQuestion(any(Question.class)))
                .thenAnswer(invocation -> answers.containsKey(((Question) invocation.getArgument(0)).getId()));
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> {
            Answer answer = invocation.getArgument(0);
            answers.put(answer.getQuestion().getId(), answer);
            return answer;
        });
        when(answerRepository.findByQuestion(any(Question.class)))
                .thenAnswer(invocation ->
                        Optional.ofNullable(answers.get(((Question) invocation.getArgument(0)).getId())));

        ElicitationService.ElicitationView view = service.fastFinish(1L);

        assertThat(view.complete()).isTrue();
        assertThat(view.currentQuestion()).isNull();
        assertThat(goal.getQuestionsAsked()).isEqualTo(goal.getMaxQuestions());
        assertThat(title.getQuestionsAsked()).isEqualTo(1);
        assertThat(titleSlot.getValue()).isNotBlank();
    }

    private ProjectCategory categoryRow(RequirementCategory category, TaxonomyCatalog.Definition definition) {
        ProjectCategory categoryRow = new ProjectCategory();
        categoryRow.setProject(project);
        categoryRow.setCategory(category);
        categoryRow.setMandatory(definition.mandatory());
        categoryRow.setMaxQuestions(definition.maxQuestions());
        return categoryRow;
    }

    private RequirementSlot slot(RequirementCategory category) {
        RequirementSlot slot = new RequirementSlot();
        slot.setProject(project);
        slot.setCategory(category);
        slot.setCompleteness(0.0);
        slot.setAssessmentJson("{}");
        return slot;
    }

    private void configureCategory(RequirementCategory category) {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(category);
        ProjectCategory categoryRow = new ProjectCategory();
        categoryRow.setProject(project);
        categoryRow.setCategory(category);
        categoryRow.setMandatory(definition.mandatory());
        categoryRow.setMaxQuestions(definition.maxQuestions());

        RequirementSlot slot = new RequirementSlot();
        slot.setProject(project);
        slot.setCategory(category);
        slot.setCompleteness(0.0);
        slot.setAssessmentJson("{}");

        when(categoryRepository.findByProjectOrderByIdAsc(project)).thenReturn(List.of(categoryRow));
        when(slotRepository.findByProjectOrderByCategoryAsc(project)).thenReturn(List.of(slot));
        when(slotRepository.findByProjectAndCategory(project, category)).thenReturn(Optional.of(slot));
    }
}
