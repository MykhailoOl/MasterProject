package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.ObjectMapper;

class EvidenceIntegrityTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final LlmCredentialService llm = mock(LlmCredentialService.class);
    private final EvidenceAssessmentService service = new EvidenceAssessmentService(llm, mapper,
            new InterviewDocumentCodec(mapper), mock(AppLog.class));

    @Test
    void wellFormedEmptyPatchCannotDigestAnySource() {
        when(llm.completeForProject(any(), eq("ASSESSMENT"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"capabilities\":[],\"entries\":[]}");
        var result = service.assess(new Project(), InterviewDocument.empty(), List.of(new Source("IDEA", "", "Save my notes.")));
        assertThat(result.processedSources()).isEmpty();
        assertThat(result.warnings()).isNotEmpty();
        verify(llm, never()).completeForProject(any(), eq("EVIDENCE_CHECK"), anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    void sourceAccountingMustBeExhaustiveUniqueAndReferToAnExtraction() {
        String patch = extraction("Save notes.", "Save notes.");
        assertThatThrownBy(() -> service.merge(InterviewDocument.empty(), patch,
                List.of(new Source("IDEA", "", "Save notes."), new Source("A2", "Who?", "Students."))))
                .hasMessageContaining("Unaccounted");
        String empty = mapper.writeValueAsString(Map.of("capabilities", List.of(), "entries", List.of(),
                "sources", List.of(Map.of("source", "IDEA", "outcome", "EXTRACTED", "reason", "Claimed success"))));
        assertThatThrownBy(() -> service.merge(InterviewDocument.empty(), empty, List.of(new Source("IDEA", "", "Save notes."))))
                .hasMessageContaining("without an extracted record");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "No account should be required.|account|Every user must create an account.",
            "Only teachers may delete a note.|delete a note|Everyone may delete a note.",
            "I am unsure whether payments are needed.|payments|The app must accept payments."
    })
    void adversarialSubstringCannotDiscardNegationScopeOrUncertainty(String original, String excerpt, String invention) {
        assertThatThrownBy(() -> service.merge(InterviewDocument.empty(), extraction(invention, excerpt),
                List.of(new Source("IDEA", "", original)))).hasMessageContaining("sentence context");
    }

    @Test
    void wholeSentenceQuoteStillNeedsIndependentSemanticSupport() {
        String original = "No account should be required.";
        when(llm.completeForProject(any(), eq("ASSESSMENT"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(extraction("Every user must create an account.", original));
        when(llm.completeForProject(any(), eq("EVIDENCE_CHECK"), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("{\"claims\":[{\"id\":\"R1\",\"supported\":false,\"reason\":\"Contradicts the explicit account exclusion\"}],\"sources\":[]}");
        var result = service.assess(new Project(), InterviewDocument.empty(), List.of(new Source("IDEA", "", original)));
        assertThat(result.entries()).isEmpty();
        assertThat(result.processedSources()).isEmpty();
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void successfulExtractionIsNotProcessedUntilEveryVerifierVerdictArrives() {
        var candidate = service.merge(InterviewDocument.empty(), extraction("Save notes.", "Save notes."),
                List.of(new Source("IDEA", "", "Save notes.")));
        assertThat(candidate.hasCheckedSource("IDEA")).isFalse();
        assertThatThrownBy(() -> service.verify(candidate, "{\"claims\":[],\"sources\":[]}"))
                .hasMessageContaining("Missing evidence verdict");
        String valid = "{\"claims\":[{\"id\":\"R1\",\"supported\":true,\"reason\":\"Explicit request to save notes\"}],"
                + "\"sources\":[{\"source\":\"IDEA\",\"complete\":true,\"reason\":\"The only request is represented\"}]}";
        var checked = service.verify(candidate, valid);
        assertThat(checked.hasCheckedSource("IDEA")).isTrue();
        assertThat(checked.sourceChecks()).allMatch(SourceCheck::verified);
    }

    @Test
    void explicitNoChangeAlsoRequiresIndependentCompletenessCheck() {
        String patch = "{\"capabilities\":[],\"entries\":[],\"sources\":[{\"source\":\"A2\",\"outcome\":\"NO_CHANGE\",\"reason\":\"No additional decisions\"}]}";
        var candidate = service.merge(InterviewDocument.empty(), patch, List.of(new Source("A2", "Anything else?", "Nothing else.")));
        assertThat(candidate.processedSources()).isEmpty();
        assertThatThrownBy(() -> service.verify(candidate, "{\"claims\":[],\"sources\":[{\"source\":\"A2\",\"complete\":false,\"reason\":\"An omitted decision exists\"}]}"))
                .hasMessageContaining("rejected");
        var checked = service.verify(candidate, "{\"claims\":[],\"sources\":[{\"source\":\"A2\",\"complete\":true,\"reason\":\"No new decision in this answer\"}]}");
        assertThat(checked.hasCheckedSource("A2")).isTrue();
    }

    @Test
    void legacyOriginCannotBeLaunderedThroughTheExtractor() {
        assertThatThrownBy(() -> service.assess(new Project(), InterviewDocument.empty(),
                List.of(new Source("A1", "Who?", "Generated administrator role", "LEGACY_UNKNOWN"))))
                .hasMessageContaining("legacy sources");
        verifyNoInteractions(llm);
    }

    private String extraction(String statement, String quote) {
        return mapper.writeValueAsString(Map.of("capabilities", List.of(), "entries", List.of(Map.of(
                "id", "", "category", "GOAL", "capabilityId", "", "criterion", "problem", "text", statement,
                "resolution", "CAPTURED", "evidence", List.of(Map.of("source", "IDEA", "quote", quote)))),
                "sources", List.of(Map.of("source", "IDEA", "outcome", "EXTRACTED", "reason", "Explicit need recorded"))));
    }
}
