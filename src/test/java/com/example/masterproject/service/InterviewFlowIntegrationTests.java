package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static com.example.masterproject.service.InterviewFixtures.*;

import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.*;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import com.example.masterproject.repository.*;
import com.example.masterproject.web.dto.CreateProjectRequest;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "interview-owner@example.com")
class InterviewFlowIntegrationTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("INTERVIEW_TEST_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            properties.add("spring.datasource.url", () -> url);
            properties.add("spring.datasource.username", () -> "postgres");
            properties.add("spring.datasource.password", () -> "");
            properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            properties.add("spring.flyway.enabled", () -> true);
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ProjectService projects;
    @Autowired InterviewStore store;
    @Autowired ElicitationService elicitation;
    @Autowired SpecExportService exports;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projectRepository;
    @Autowired InterviewRevisionRepository revisions;
    @Autowired ExportArtifactRepository artifacts;
    @Autowired CompletenessSnapshotRepository snapshots;
    @Autowired StudyAssignmentService assignments;
    @MockitoBean LlmCredentialService llm;

    @BeforeEach
    void prepare() {
        users.findByEmail("another@example.com").orElseGet(() -> {
            User user = new User(); user.setEmail("another@example.com"); user.setUsername("otherowner");
            user.setPasswordHash("unused-test-password-hash"); user.setRole(UserRole.USER);
            return users.save(user);
        });
        users.findByEmail("interview-owner@example.com").orElseGet(() -> {
            User user = new User(); user.setEmail("interview-owner@example.com"); user.setUsername("interviewowner");
            user.setPasswordHash("unused-test-password-hash"); user.setRole(UserRole.USER);
            return users.save(user);
        });
        when(llm.hasProvider(any())).thenReturn(true);
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return modelResponse(invocation);
                });
    }

    @Test
    void noviceCanStartAnswerReviewConfirmAndDownloadWithoutGeneratedAnswers() throws Exception {
        Long id = project();
        perform(get("/projects/{id}/elicit", id)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check notes and continue")));
        verify(llm, never()).completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        perform(post("/projects/{id}/elicit/next", id).with(csrf())).andExpect(status().is3xxRedirection());
        Question question = store.load(id).unanswered();
        assertThat(question).isNotNull();
        perform(get("/projects/{id}/elicit", id)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("I'm not sure yet")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Use example"))));
        perform(post("/projects/{id}/elicit/{qid}", id, question.getId()).with(csrf())
                .param("answerAction", "UNSURE")).andExpect(status().is3xxRedirection());
        assertThat(store.load(id).answers()).hasSize(1);
        assertThat(store.load(id).answers().getFirst().getAnswerText()).contains("do not know yet");
        finish(id);
        perform(get("/projects/{id}/review", id)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("save and find notes")));
        long revision = store.load(id).project().getInterviewRevision();
        perform(post("/projects/{id}/review/confirm", id).with(csrf())
                .param("revision", Long.toString(revision)).param("title", "My notes"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(store.load(id).project().getInterviewRevision()).isEqualTo(revision);
        perform(post("/projects/{id}/review/confirm", id).with(csrf())
                .param("revision", Long.toString(revision)).param("title", "My notes").param("accepted", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(store.load(id).project().getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        clearInvocations(llm);
        String first = perform(get("/projects/{id}/export/spec/download", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = perform(get("/projects/{id}/export/spec/download", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(first).isEqualTo(second).contains("reviewed by the project owner", "save and find notes");
        assertThat(first).contains("## Summary\nsave and find notes\n\n## How to use");
        verifyNoInteractions(llm);
    }

    @Test
    void answerCommitsBeforeModelCallAndFailureDoesNotGenerateCompletionScore() {
        Long id = project();
        Question question = elicitation.getOrAdvance(id).currentQuestion();
        long scoreCount = snapshots.count();
        when(llm.completeForProject(any(), eq("ASSESSMENT"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    assertThat(store.load(id).answers()).extracting(Answer::getAnswerText).contains("Students will use it.");
                    throw new IllegalStateException("Temporary outage");
                });
        elicitation.submitAnswer(id, question.getId(), "Students will use it.");
        var saved = store.load(id);
        assertThat(saved.assessed()).isFalse();
        assertThat(saved.answers()).hasSize(1);
        assertThat(saved.document().entries()).hasSize(1);
        assertThat(snapshots.count()).isEqualTo(scoreCount);
        finish(id);
        assertThatThrownBy(() -> store.confirm(id, store.load(id).project().getInterviewRevision(), "Notes", true))
                .hasMessageContaining("still need checking");
        when(llm.completeForProject(any(), eq("ASSESSMENT"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"capabilities\":[],\"entries\":[]}");
        assertThat(elicitation.refreshAssessment(id).assessed()).isFalse();
        assertThat(snapshots.count()).isEqualTo(scoreCount);
        assertThatThrownBy(() -> store.confirm(id, store.load(id).project().getInterviewRevision(), "Notes", true))
                .hasMessageContaining("still need checking");
        when(llm.completeForProject(any(), eq("ASSESSMENT"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenAnswer(this::modelResponse);
        assertThat(elicitation.refreshAssessment(id).assessed()).isTrue();
        assertThat(snapshots.count()).isEqualTo(scoreCount + 1);
    }

    @Test
    void duplicateAnswerAndConcurrentAnswerAreIdempotent() throws Exception {
        Long id = project();
        Question question = elicitation.getOrAdvance(id).currentQuestion();
        var context = SecurityContextHolder.getContext();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> action = () -> {
                SecurityContextHolder.setContext(context);
                try { store.answer(id, question.getId(), "Students."); return null; }
                finally { SecurityContextHolder.clearContext(); }
            };
            Future<Void> first = executor.submit(action);
            Future<Void> second = executor.submit(action);
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
        }
        assertThat(store.load(id).answers()).hasSize(1);
        assertThatThrownBy(() -> store.answer(id, question.getId(), "Different answer."))
                .hasMessageContaining("already answered");
        assertThat(revisions.findByProjectIdOrderByRevisionAsc(id).stream()
                .filter(r -> r.getEventType().startsWith("ANSWER:"))).hasSize(1);
    }

    @Test
    void staleReviewCannotOverwriteCorrectionAndPreviousExportBecomesUnavailable() throws Exception {
        Long id = project(); elicitation.getOrAdvance(id); finish(id);
        store.confirm(id, store.load(id).project().getInterviewRevision(), "Notes", true);
        ExportArtifact artifact = exports.generateSpecMarkdown(id);
        long reviewed = store.load(id).project().getInterviewRevision();
        store.correct(id, reviewed, "Keep my notes private.");
        assertThat(exports.latestSpec(id)).isNull();
        assertThat(artifacts.findById(artifact.getId())).isPresent();
        assertThatThrownBy(() -> store.confirm(id, reviewed, "Stale name", true)).hasMessageContaining("another request");
        perform(get("/projects/{id}/export/spec/download", id)).andExpect(status().isConflict());
        assertThat(revisions.findByProjectIdOrderByRevisionAsc(id)).extracting(InterviewRevision::getEventType)
                .contains("REVIEW_CONFIRMED").anyMatch(event -> event.startsWith("CORRECTION:"));
        assertThat(elicitation.refreshAssessment(id).assessed()).isTrue();
        store.confirm(id, store.load(id).project().getInterviewRevision(), "Private notes", true);
        var updated = exports.generateSpecMarkdown(id);
        assertThat(updated.getContent()).contains("## Summary\nsave and find notes Keep my notes private.\n\n## How to use");
        assertThat(updated.getSourceRevision()).isGreaterThan(artifact.getSourceRevision());
        assertThat(artifacts.findById(artifact.getId()).orElseThrow().getContent()).isEqualTo(artifact.getContent());
    }

    @Test
    void ownershipAndCsrfProtectInterviewReviewAndExport() throws Exception {
        Long id = project();
        perform(post("/projects/{id}/elicit/next", id)).andExpect(status().isForbidden());
        perform(get("/projects/{id}/review", id).with(user("another@example.com")))
                .andExpect(status().is3xxRedirection());
        perform(post("/projects/{id}/review/correct", id).with(user("another@example.com")).with(csrf())
                .param("revision", "0").param("correction", "Take over this project"))
                .andExpect(status().is3xxRedirection());
        assertThat(store.load(id).answers()).isEmpty();
        assertThat(store.load(id).project().getInterviewRevision()).isZero();
    }

    @Test
    void staleModelResponseCannotReplaceAnAnswerSavedInAnotherRequest() {
        Long id = project(); Question question = elicitation.getOrAdvance(id).currentQuestion();
        var old = store.load(id);
        store.answer(id, question.getId(), "Students.");
        assertThatThrownBy(() -> store.saveDocument(id, old.project().getInterviewRevision(), old.document()))
                .hasMessageContaining("another request");
        assertThat(store.load(id).assessed()).isFalse();
    }

    @Test
    void legacyProjectIsReadWithoutOverwritingPriorDataAndRequiresReview() throws Exception {
        Long id = project();
        Project project = projectRepository.findById(id).orElseThrow();
        project.setInterviewDocument(null); project.setCollectionProtocol("LEGACY_UNVERIFIED");
        project.setStatus(ProjectStatus.COMPLETED); projectRepository.save(project);
        perform(get("/projects/{id}/review", id)).andExpect(status().isOk());
        assertThat(projectRepository.findById(id).orElseThrow().getInterviewDocument()).isNull();
        assertThatThrownBy(() -> exports.generateSpecMarkdown(id)).hasMessageContaining("Review and confirm");
        clearInvocations(llm);
        assertThatThrownBy(() -> elicitation.refreshAssessment(id)).hasMessageContaining("unverified source provenance");
        assertThatThrownBy(() -> store.confirm(id, store.load(id).project().getInterviewRevision(), "Old notes", true))
                .hasMessageContaining("older collection method");
        verifyNoInteractions(llm);
    }

    @Test
    void participantsCannotChooseTheirStudyCondition() {
        CreateProjectRequest request = request(); request.setStudyCondition(StudyCondition.BASELINE);
        assertThatThrownBy(() -> projects.createProject(request)).hasMessageContaining("Admin access required");
    }

    @Test
    void choosingUnsureKeepsAnyPartialAnswerAlreadyTyped() throws Exception {
        Long id = project(); Question question = elicitation.getOrAdvance(id).currentQuestion();
        perform(post("/projects/{id}/elicit/{qid}", id, question.getId()).with(csrf())
                .param("answerAction", "UNSURE").param("answerText", "Probably students, but I need to ask them."))
                .andExpect(status().is3xxRedirection());
        assertThat(store.load(id).answers().getFirst().getAnswerText())
                .contains("Probably students, but I need to ask them.", "remaining details");
    }

    @Test
    void researcherAssignmentAppliesToNewSessionsWithoutChangingExistingOnes() throws Exception {
        User owner = users.findByEmail("interview-owner@example.com").orElseThrow();
        Long guided = project();
        owner.setStudyCondition(StudyCondition.BASELINE); users.save(owner);
        try {
            Long baseline = project();
            assertThat(store.load(baseline).session().getConditionTag()).isEqualTo(StudyCondition.BASELINE);
            assertThat(store.load(guided).session().getConditionTag()).isEqualTo(StudyCondition.GUIDED);
            assertThatThrownBy(() -> assignments.assign(owner.getId(), StudyCondition.GUIDED)).hasMessageContaining("Admin access required");
            perform(post("/admin/users/{id}/study-condition", owner.getId()).with(csrf()).param("condition", "GUIDED"))
                    .andExpect(status().isForbidden());
        } finally { owner.setStudyCondition(null); users.save(owner); }
    }

    @Test
    @WithMockUser(username = "research-admin@example.com", roles = "ADMIN")
    void administratorCanAssignParticipantsAndExportTheFullRevisionHistory() throws Exception {
        users.findByEmail("research-admin@example.com").orElseGet(() -> {
            User admin = new User(); admin.setEmail("research-admin@example.com"); admin.setUsername("researchadmin");
            admin.setPasswordHash("unused-admin-hash"); admin.setRole(UserRole.ADMIN); return users.save(admin);
        });
        User owner = users.findByEmail("interview-owner@example.com").orElseThrow();
        try {
            perform(post("/admin/users/{id}/study-condition", owner.getId()).with(csrf()).param("condition", "BASELINE"))
                    .andExpect(status().is3xxRedirection());
            assertThat(assignments.assignment(owner.getId())).isEqualTo(StudyCondition.BASELINE);
            perform(get("/admin/users/{id}", owner.getId())).andExpect(status().isOk());
            Long id = project(); elicitation.getOrAdvance(id); finish(id);
            store.confirm(id, store.load(id).project().getInterviewRevision(), "Notes", true);
            exports.generateSpecMarkdown(id);
            String json = perform(get("/admin/exports/study-data.json").param("includeExcluded", "true")).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(json).contains("REVIEW_CONFIRMED", "ideaspec-interview-3", "sourceRevision", "questionKind", "generationOrigin", "provenance")
                    .doesNotContain("unused-admin-hash", "apiKeyEnc", "passwordHash");
            perform(get("/admin/exports/study-data-csv.zip")).andExpect(status().isOk());
        } finally { owner.setStudyCondition(null); owner.setAssignmentMethod("UNASSIGNED"); users.save(owner); }
    }

    @Test
    @WithMockUser(username = "research-admin@example.com", roles = "ADMIN")
    void randomizedEnrollmentIsFixedAndStudyExportRetainsFailedSessionsButExcludesLegacyAndOrdinaryUse() throws Exception {
        User admin = users.findByEmail("research-admin@example.com").orElseGet(() -> {
            User user = new User(); user.setEmail("research-admin@example.com"); user.setUsername("researchadmin");
            user.setPasswordHash("unused-admin-hash"); user.setRole(UserRole.ADMIN); return users.save(user);
        });
        admin.setStudyCondition(null); admin.setAssignmentMethod("UNASSIGNED"); users.save(admin);
        Long ordinary = project();
        try {
            perform(post("/admin/users/{id}/enroll", admin.getId()).with(csrf())).andExpect(status().is3xxRedirection());
            var assigned = users.findById(admin.getId()).orElseThrow();
            assertThat(assigned.getAssignmentMethod()).isEqualTo("RANDOMIZED");
            assertThat(assigned.getStudyCondition()).isNotNull();
            assertThatThrownBy(() -> assignments.randomize(admin.getId())).hasMessageContaining("already has an assignment");
            assertThatThrownBy(() -> assignments.assign(admin.getId(), StudyCondition.GUIDED)).hasMessageContaining("fixed");
            Long randomized = project();
            assertThat(store.load(randomized).session().isStudyEnrolled()).isTrue();
            assertThat(store.load(randomized).session().getStudyModel()).isEqualTo("gpt-5-mini");
            when(llm.completeForProject(any(), startsWith("INTERVIEW_"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                    .thenThrow(new IllegalStateException("Quota", new org.springframework.web.client.RestClientResponseException(
                            "Too Many Requests", 429, "Too Many Requests", null, null, null)));
            Question fallback = elicitation.getOrAdvance(randomized).currentQuestion();
            assertThat(fallback.getGenerationOrigin()).isEqualTo("LOCAL_FALLBACK");
            assertThat(fallback.getGenerationReason()).isEqualTo("QUOTA_OR_RATE_LIMIT");
            assertThat(fallback.getLlmCallId()).isNotBlank();
            Long legacy = project();
            var old = projectRepository.findById(legacy).orElseThrow(); old.setCollectionProtocol("LEGACY_UNVERIFIED"); projectRepository.save(old);
            String raw = perform(get("/admin/exports/study-data.json")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var data = new tools.jackson.databind.ObjectMapper().readTree(raw);
            var exportedIds = new java.util.ArrayList<Long>();
            data.path("projects").forEach(node -> exportedIds.add(node.path("id").asLong()));
            assertThat(exportedIds).contains(randomized).doesNotContain(ordinary, legacy);
            assertThat(raw).contains("QUOTA_OR_RATE_LIMIT", "INTENTION_TO_TREAT", "LEGACY_COLLECTION_PROTOCOL", "NOT_RANDOMIZED_STUDY_SESSION");
            assertThat(store.load(randomized).project().getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        } finally {
            admin.setStudyCondition(null); admin.setAssignmentMethod("UNASSIGNED"); users.save(admin);
        }
    }

    @Test
    @WithMockUser(username = "research-admin@example.com", roles = "ADMIN")
    void explicitGuidedAdminChoiceOverridesBaselineAssignment() throws Exception {
        User admin = users.findByEmail("research-admin@example.com").orElseGet(() -> {
            User user = new User(); user.setEmail("research-admin@example.com"); user.setUsername("researchadmin");
            user.setPasswordHash("unused-admin-hash"); user.setRole(UserRole.ADMIN); return users.save(user);
        });
        admin.setStudyCondition(StudyCondition.BASELINE); admin.setAssignmentMethod("MANUAL"); users.save(admin);
        try {
            perform(get("/projects/new")).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"GUIDED\"")));
            var request = request(); request.setStudyCondition(StudyCondition.GUIDED);
            Long id = projects.createProject(request).getId();
            assertThat(store.load(id).session().getConditionTag()).isEqualTo(StudyCondition.GUIDED);
            assertThat(store.load(id).session().isStudyEnrolled()).isFalse();
            assertThat(store.load(id).session().getAssignmentMethod()).isEqualTo("ADMIN_PREVIEW");
        } finally { admin.setStudyCondition(null); admin.setAssignmentMethod("UNASSIGNED"); users.save(admin); }
    }

    private Long project() { return projects.createProject(request()).getId(); }
    private org.springframework.test.web.servlet.ResultActions perform(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        var context = SecurityContextHolder.getContext();
        try { return mvc.perform(request); }
        finally { SecurityContextHolder.setContext(context); }
    }
    private CreateProjectRequest request() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setInitialIdea("save and find notes"); request.setLlmProvider(LlmProvider.OPENAI);
        request.setSimplifyModeEnabled(false); return request;
    }
    private String modelResponse(org.mockito.invocation.InvocationOnMock invocation) {
        var mapper = new tools.jackson.databind.ObjectMapper();
        if (invocation.<String>getArgument(1).equals("EVIDENCE_CHECK")) {
            String context = invocation.getArgument(4);
            String record = context.substring(context.indexOf("\nProposed complete record:\n") + "\nProposed complete record:\n".length());
            var doc = mapper.readValue(record, InterviewDocument.class);
            var ids = new java.util.ArrayList<String>();
            doc.capabilities().forEach(c -> ids.add(c.id()));
            doc.entries().forEach(e -> ids.add(e.id()));
            return mapper.writeValueAsString(java.util.Map.of("claims", ids.stream()
                    .map(key -> java.util.Map.of("id", key, "supported", true, "reason", "Explicit fixture decision")).toList(),
                    "sources", doc.sourceChecks().stream().map(s -> java.util.Map.of("source", s.source(),
                            "complete", true, "reason", "All fixture decisions represented")).toList()));
        }
        if (!invocation.<String>getArgument(1).equals("ASSESSMENT")) {
            return "{\"question\":\"Who would you like to use this?\",\"done\":false}";
        }
        var snapshot = store.load(invocation.<Project>getArgument(0).getId());
        var pending = snapshot.sources().stream().filter(s -> !snapshot.document().hasCheckedSource(s.id())).toList();
        var entries = pending.stream().map(s -> new Entry("", RequirementCategory.GOAL, "", "problem", s.text(),
                s.text().contains("unsure") || s.text().contains("do not know") ? Resolution.DEFERRED : Resolution.CAPTURED,
                List.of(new Evidence(s.id(), s.text())))).toList();
        return mapper.writeValueAsString(java.util.Map.of("capabilities", List.of(), "entries", entries,
                "sources", pending.stream().map(s -> java.util.Map.of("source", s.id(), "outcome", "EXTRACTED", "reason", "Fixture decision recorded")).toList()));
    }
    private void finish(Long id) { store.finish(id, store.load(id).project().getInterviewRevision()); }
}

