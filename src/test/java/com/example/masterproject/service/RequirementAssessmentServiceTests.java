package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.Question;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RequirementAssessmentServiceTests {

    private final RequirementSlotRepository slotRepository = mock(RequirementSlotRepository.class);
    private final LlmCredentialService llmCredentialService = mock(LlmCredentialService.class);
    private final AppLog appLog = mock(AppLog.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GuidedElicitationPlanner planner = new GuidedElicitationPlanner(objectMapper);
    private final RequirementAssessmentService service = new RequirementAssessmentService(
            slotRepository,
            llmCredentialService,
            planner,
            objectMapper,
            appLog);
    private Project project;
    private RequirementSlot slot;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setLlmProvider(LlmProvider.OPENAI);
        project.setInitialIdea("A web application that helps students track assignments.");

        slot = new RequirementSlot();
        slot.setProject(project);
        slot.setCategory(RequirementCategory.GOAL);
        slot.setAssessmentJson(service.emptyAssessmentJson(
                com.example.masterproject.model.taxonomy.TaxonomyCatalog.require(RequirementCategory.GOAL)));
    }

    @Test
    void initialIdeaIsExtractedWithoutAddingUnrequestedCategories() throws Exception {
        when(llmCredentialService.complete(
                        eq(LlmProvider.OPENAI),
                        anyString(),
                        anyString(),
                        eq(0.0),
                        anyInt()))
                .thenReturn("""
                        {
                          "categories": [
                            {
                              "category": "GOAL",
                              "value": "Students can track assignments.",
                              "statuses": {
                                "problem": "COVERED",
                                "outcome": "COVERED",
                                "success": "PARTIAL",
                                "priority": "MISSING"
                              }
                            },
                            {
                              "category": "INTEGRATIONS",
                              "value": "Invented calendar integration",
                              "statuses": {}
                            }
                          ]
                        }
                        """);

        service.initializeFromIdea(project, List.of(slot));

        JsonNode assessment = objectMapper.readTree(slot.getAssessmentJson());
        assertThat(slot.getValue()).isEqualTo("Students can track assignments.");
        assertThat(slot.getCompleteness()).isEqualTo(0.625);
        assertThat(assessment.get("problem").asText()).isEqualTo("COVERED");
        assertThat(assessment.get("priority").asText()).isEqualTo("MISSING");
        verify(slotRepository).saveAll(List.of(slot));
    }

    @Test
    void answerScoreIsComputedLocallyFromCriterionStatuses() throws Exception {
        Question question = question("problem");
        when(llmCredentialService.complete(
                        eq(LlmProvider.OPENAI),
                        anyString(),
                        anyString(),
                        anyDouble(),
                        anyInt()))
                .thenReturn("""
                        {
                          "value": "Students miss assignment deadlines and need a single tracking view.",
                          "statuses": {
                            "problem": "COVERED",
                            "outcome": "PARTIAL",
                            "success": "MISSING",
                            "priority": "MISSING"
                          }
                        }
                        """);

        service.assessAnswer(project, slot, question, "I often miss deadlines.");

        assertThat(slot.getValue())
                .isEqualTo("Students miss assignment deadlines and need a single tracking view.");
        assertThat(slot.getCompleteness()).isEqualTo(0.375);
        verify(slotRepository).save(slot);
    }

    @Test
    void malformedAssessmentPreservesTheAnswerAndAdvancesTheFocusedCriterion() {
        Question question = question("problem");
        when(llmCredentialService.complete(
                        eq(LlmProvider.OPENAI),
                        anyString(),
                        anyString(),
                        anyDouble(),
                        anyInt()))
                .thenReturn("not-json");

        service.assessAnswer(project, slot, question, "Students miss deadlines.");

        assertThat(slot.getValue()).isEqualTo("Students miss deadlines.");
        assertThat(slot.getCompleteness()).isEqualTo(0.125);
    }

    private Question question(String focusCriterion) {
        ElicitationSession session = new ElicitationSession();
        session.setProject(project);
        Question question = new Question();
        question.setSession(session);
        question.setCategory(RequirementCategory.GOAL);
        question.setQuestionText("What problem should this product solve?");
        question.setFocusCriterion(focusCriterion);
        return question;
    }
}
