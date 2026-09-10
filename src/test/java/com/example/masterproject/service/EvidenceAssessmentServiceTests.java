package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.masterproject.service.InterviewFixtures.*;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class EvidenceAssessmentServiceTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final LlmCredentialService llm = mock(LlmCredentialService.class);
    private final EvidenceAssessmentService assessment = new EvidenceAssessmentService(llm, mapper,
            new InterviewDocumentCodec(mapper), mock(AppLog.class));

    @Test
    void capturesSeparateCapabilitiesAndFactsAcrossCategoriesFromOneAnswer() {
        String raw = """
                {"capabilities":[
                {"id":"","name":"Save notes","evidence":[{"source":"IDEA","quote":"I want to save notes and find notes."}]},
                {"id":"","name":"Find notes","evidence":[{"source":"IDEA","quote":"I want to save notes and find notes."}]}],
                "entries":[
                {"id":"","category":"CORE_FEATURES","capabilityId":"Save notes","criterion":"acceptance",
                "text":"A saved note appears in the list.","resolution":"CAPTURED",
                "evidence":[{"source":"IDEA","quote":"A saved note appears in the list."}]},
                {"id":"","category":"AUTHENTICATION","capabilityId":"","criterion":"_applicable",
                "text":"No accounts are needed.","resolution":"NOT_APPLICABLE",
                "evidence":[{"source":"IDEA","quote":"No accounts are needed."}]}],
                "sources":[{"source":"IDEA","outcome":"EXTRACTED","reason":"Capabilities and account exclusion recorded"}]}
                """;
        var result = assessment.merge(InterviewDocument.empty(), raw, List.of(new Source("IDEA", "",
                "I want to save notes and find notes. A saved note appears in the list. No accounts are needed.")));
        assertThat(result.capabilities()).extracting(Capability::id).containsExactly("C1", "C2");
        assertThat(result.entries().getFirst().capabilityId()).isEqualTo("C1");
        assertThat(result.entries().getLast().category()).isEqualTo(RequirementCategory.AUTHENTICATION);
        assertThat(new InterviewPlanner().coverage(result, RequirementCategory.CORE_FEATURES)).isLessThan(0.5);
    }

    @Test
    void sameStatusCorrectionReplacesOldFactAndPreservesOmittedDetails() {
        Entry old = fact("R1", RequirementCategory.GOAL, "", "problem", "Remember deadlines.", Resolution.CAPTURED);
        Entry other = fact("R2", RequirementCategory.GOAL, "", "outcome", "Keep all notes together.", Resolution.CAPTURED);
        var prior = document(List.of(), old, other);
        Entry changed = new Entry("R1", RequirementCategory.GOAL, "", "problem", "Remember appointments.",
                Resolution.CAPTURED, List.of(new Evidence("A2", "Remember appointments.")));
        var result = assessment.merge(prior, patch(changed), List.of(new Source("IDEA", "", "save and find notes"),
                new Source("A2", "What should change?", "Remember appointments.")));
        assertThat(result.entries()).containsExactly(changed, other);
        assertThat(prior.entries()).containsExactly(old, other);
    }

    @Test
    void rejectsEvidenceCopiedFromAQuestionOrInventedSource() {
        Entry unsupported = new Entry("", RequirementCategory.USERS_AND_ROLES, "", "managers", "Add an administrator.",
                Resolution.CAPTURED, List.of(new Evidence("IDEA", "administrator")));
        assertThatThrownBy(() -> assessment.merge(InterviewDocument.empty(), patch(unsupported),
                List.of(new Source("IDEA", "Should an administrator help?", "I do not know."))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported source");
        assertThatThrownBy(() -> assessment.merge(InterviewDocument.empty(), patch(unsupported), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotRewriteKnownFactsUsingOnlyOldEvidence() {
        var prior = document(List.of(), fact("R1", RequirementCategory.GOAL, "", "problem", "Save notes.", Resolution.CAPTURED));
        var replacement = fact("R1", RequirementCategory.GOAL, "", "problem", "Delete notes.", Resolution.CAPTURED);
        assertThatThrownBy(() -> assessment.merge(prior, patch(replacement),
                List.of(new Source("IDEA", "", "save and find notes"))))
                .hasMessageContaining("no new evidence");
    }

    @Test
    void failedExtractionPreservesFactsAndLeavesNewAnswerUnprocessed() {
        var prior = document(List.of(), fact("R1", RequirementCategory.GOAL, "", "problem", "Save notes.", Resolution.CAPTURED));
        when(llm.completeForProject(any(), anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new IllegalStateException("provider unavailable"));
        var result = assessment.assess(new Project(), prior, List.of(new Source("IDEA", "", "save and find notes"),
                new Source("A2", "Who uses it?", "Students.")));
        assertThat(result.entries()).isEqualTo(prior.entries());
        assertThat(result.processedSources()).containsExactly("IDEA");
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void omissionNeverPrunesLongRecordsAndTruncatedJsonNeverReplacesThem() {
        String longFact = "Keep the full details. ".repeat(250);
        var prior = document(List.of(), fact("R1", RequirementCategory.GOAL, "", "problem", longFact, Resolution.CAPTURED));
        var sources = List.of(new Source("IDEA", "", "save and find notes"), new Source("A2", "Anything else?", "Not yet."));
        assertThat(assessment.merge(prior, "{\"capabilities\":[],\"entries\":[],\"sources\":[{\"source\":\"A2\",\"outcome\":\"NO_CHANGE\",\"reason\":\"No additional needs\"}]}", sources).entries().getFirst().text())
                .isEqualTo(longFact).hasSizeGreaterThan(5000);
        assertThatThrownBy(() -> assessment.merge(prior, "{\"entries\":[", sources)).isInstanceOf(RuntimeException.class);
        assertThat(prior.entries().getFirst().text()).isEqualTo(longFact);
    }

    @Test
    void unknownAndAmbiguousCapabilityIdentifiersAreRejected() {
        var prior = document(CAPABILITIES);
        String raw = """
                {"capabilities":[{"id":"C999","name":"Save notes",
                "evidence":[{"source":"IDEA","quote":"save and find notes"}]}],"entries":[]}
                """;
        assertThatThrownBy(() -> assessment.merge(prior, raw,
                List.of(new Source("IDEA", "", "save and find notes")))).hasMessageContaining("Unknown capability");
    }

    @Test
    void conflictKeepsBothSourcesUntilExplicitlyResolved() {
        var conflict = new Entry("", RequirementCategory.USERS_AND_ROLES, "", "customers",
                "Is this for students or teachers?", Resolution.CONFLICT,
                List.of(new Evidence("IDEA", "Students"), new Evidence("A2", "Teachers")));
        var result = assessment.merge(InterviewDocument.empty(), patch(conflict),
                List.of(new Source("IDEA", "", "Students"), new Source("A2", "Who uses it?", "Teachers")));
        assertThat(result.entries().getFirst().evidence()).hasSize(2);
        assertThat(new InterviewPlanner().next(result, List.of()).orElseThrow().priority()).isZero();
    }

    private String patch(Entry entry) {
        return mapper.writeValueAsString(Map.of("capabilities", List.of(), "entries", List.of(entry), "sources",
                entry.evidence().stream().map(Evidence::source).distinct()
                        .map(id -> Map.of("source", id, "outcome", "EXTRACTED", "reason", "Decision recorded")).toList()));
    }
}
