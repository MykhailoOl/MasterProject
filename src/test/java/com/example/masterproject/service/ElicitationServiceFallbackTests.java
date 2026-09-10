package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.masterproject.service.InterviewFixtures.*;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.*;
import com.example.masterproject.model.interview.InterviewDocument;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class ElicitationServiceFallbackTests {
    private final InterviewStore store = mock(InterviewStore.class);
    private final EvidenceAssessmentService assessment = mock(EvidenceAssessmentService.class);
    private final LlmCredentialService llm = mock(LlmCredentialService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ElicitationService service = new ElicitationService(store, assessment, new InterviewPlanner(), llm,
            new InterviewDocumentCodec(mapper), mapper, mock(AppLog.class), mock(StudyProtocol.class));

    @Test
    void readingInterviewDoesNotGenerateQuestionsOrAssessAnswers() {
        when(store.load(1L)).thenReturn(snapshot(StudyCondition.GUIDED, document(List.of())));
        assertThat(service.view(1L).currentQuestion()).isNull();
        verifyNoInteractions(assessment, llm);
        verify(store, never()).saveQuestion(anyLong(), anyLong(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void providerOutageUsesNeutralLocalQuestionAndDoesNotInventAnAnswer() {
        when(store.load(1L)).thenReturn(snapshot(StudyCondition.GUIDED, document(List.of())));
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new IllegalStateException("unavailable"));
        service.getOrAdvance(1L);
        verify(store).saveQuestion(eq(1L), eq(0L), any(), eq("What concrete problem or frustration should the first version remove for people?"),
                eq("LOCAL_FALLBACK"), eq("PROVIDER_OR_RESPONSE_FAILURE"), anyString());
        verify(store, never()).answer(anyLong(), anyLong(), anyString());
    }

    @Test
    void baselineQuestionerReceivesConversationWithoutGuidanceRecord() {
        var doc = document(CAPABILITIES, fact("R1", RequirementCategory.GOAL, "", "problem", "Save notes.", InterviewDocument.Resolution.CAPTURED));
        when(store.load(1L)).thenReturn(snapshot(StudyCondition.BASELINE, doc));
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"question\":\"Who would you like to use this?\",\"done\":false}");
        service.getOrAdvance(1L);
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(llm).completeForProject(any(), eq("INTERVIEW_BASELINE"), anyString(), system.capture(), user.capture(), eq(0.2), eq(1200));
        assertThat(system.getValue()).contains("strong adaptive requirements interview", "no hands-on experience");
        assertThat(user.getValue()).contains("save and find notes").doesNotContain("Recorded decisions", "Focus:", "R1", "CORE_FEATURES");
    }

    @Test
    void failedAssessmentStopsQuestionGenerationUntilRetry() {
        var snapshot = snapshot(StudyCondition.GUIDED, InterviewDocument.empty());
        when(store.load(1L)).thenReturn(snapshot);
        when(assessment.assess(any(), any(), anyList())).thenReturn(InterviewDocument.empty());
        assertThat(service.getOrAdvance(1L).needsAssessment()).isTrue();
        verifyNoInteractions(llm);
        verify(store, never()).saveQuestion(anyLong(), anyLong(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void rejectsJargonMultipleQuestionsAndRepeatedQuestions() {
        String fallback = "What should a person see?";
        assertThat(service.normalizeQuestion("Which database and API should we use?", fallback, List.of())).isEqualTo(fallback);
        assertThat(service.normalizeQuestion("Who uses it? What do they do?", fallback, List.of())).isEqualTo(fallback);
        Question previous = new Question(); previous.setQuestionText("Who uses it?");
        assertThat(service.normalizeQuestion("Who uses it?", fallback, List.of(previous))).isEqualTo(fallback);
        assertThat(service.normalizeQuestion("What would show you that it worked?", fallback, List.of()))
                .isEqualTo("What would show you that it worked?");
    }

    @Test
    void baselineDoneCannotStopBeforeTheSharedBudget() {
        var base = snapshot(StudyCondition.BASELINE, document(List.of()));
        var answers = new java.util.ArrayList<Answer>();
        var checks = new java.util.ArrayList<>(base.document().sourceChecks());
        var processed = new java.util.ArrayList<>(base.document().processedSources());
        for (long id = 1; id <= 4; id++) {
            Question q = new Question(); q.setId(id); q.setCategory(RequirementCategory.GOAL); q.setQuestionText("Earlier question " + id + "?");
            Answer a = new Answer(); a.setId(id); a.setQuestion(q); a.setAnswerText("Saved answer " + id);
            answers.add(a); processed.add("A" + id);
            checks.add(new InterviewDocument.SourceCheck("A" + id, "EXTRACTED", "Checked fixture", true));
        }
        var doc = new InterviewDocument(InterviewDocument.VERSION, List.of(), List.of(), processed, List.of(), "", checks);
        when(store.load(1L)).thenReturn(new InterviewStore.Snapshot(base.project(), base.session(),
                answers.stream().map(Answer::getQuestion).toList(), answers, doc));
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"question\":\"\",\"done\":true}");
        service.getOrAdvance(1L);
        verify(store, never()).finish(anyLong(), anyLong());
        verify(store, never()).finish(anyLong(), anyLong(), anyString());
        verify(store).saveQuestion(eq(1L), eq(0L), any(), anyString(), eq("LOCAL_FALLBACK"), eq("INVALID_WORDING"), anyString());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(StudyCondition.class)
    void bothArmsStopAtTheSameStoredBudget(StudyCondition condition) {
        var base = snapshot(condition, document(List.of()));
        var answers = new java.util.ArrayList<Answer>();
        var checks = new java.util.ArrayList<>(base.document().sourceChecks());
        var processed = new java.util.ArrayList<>(base.document().processedSources());
        for (long id = 1; id <= base.session().getQuestionBudget(); id++) {
            Question q = new Question(); q.setId(id); q.setCategory(RequirementCategory.GOAL); q.setQuestionText("Earlier question " + id + "?");
            Answer a = new Answer(); a.setId(id); a.setQuestion(q); a.setAnswerText("Saved answer " + id);
            answers.add(a); processed.add("A" + id);
            checks.add(new InterviewDocument.SourceCheck("A" + id, "EXTRACTED", "Checked fixture", true));
        }
        var doc = new InterviewDocument(InterviewDocument.VERSION, List.of(), List.of(), processed, List.of(), "", checks);
        when(store.load(1L)).thenReturn(new InterviewStore.Snapshot(base.project(), base.session(),
                answers.stream().map(Answer::getQuestion).toList(), answers, doc));
        service.getOrAdvance(1L);
        verify(store).finish(1L, 0L, "BUDGET");
        verifyNoInteractions(llm);
    }

    @Test
    void exhaustedGuidedPlannerContinuesUnderTheCommonStoppingPolicy() {
        var planner = mock(InterviewPlanner.class);
        when(planner.next(any(), anyList())).thenReturn(java.util.Optional.empty());
        var interviewer = new ElicitationService(store, assessment, planner, llm, new InterviewDocumentCodec(mapper),
                mapper, mock(AppLog.class), mock(StudyProtocol.class));
        when(store.load(1L)).thenReturn(snapshot(StudyCondition.GUIDED, document(List.of())));
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"question\":\"What might make the main task difficult?\"}");
        interviewer.getOrAdvance(1L);
        verify(store, never()).finish(anyLong(), anyLong(), anyString());
        verify(store).saveQuestion(eq(1L), eq(0L), any(), anyString(), eq("MODEL"), isNull(), anyString());
    }

    private InterviewStore.Snapshot snapshot(StudyCondition condition, InterviewDocument doc) {
        Project project = new Project(); project.setId(1L); project.setTitle("Notes");
        project.setInitialIdea("save and find notes"); project.setLlmProvider(LlmProvider.OPENAI);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        ElicitationSession session = new ElicitationSession(); session.setId(1L); session.setProject(project);
        session.setConditionTag(condition);
        return new InterviewStore.Snapshot(project, session, List.of(), List.of(), doc);
    }
}
