package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.masterproject.service.InterviewFixtures.*;
import static com.example.masterproject.model.enums.RequirementCategory.*;
import static com.example.masterproject.model.interview.InterviewDocument.Resolution.*;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.ExportType;
import com.example.masterproject.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpecExportServiceTests {
    private final SpecRenderer renderer = new SpecRenderer(new InterviewPlanner());
    private final InterviewDocumentCodec codec = new InterviewDocumentCodec(new ObjectMapper());
    private final ProjectService projects = mock(ProjectService.class);
    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final ExportArtifactRepository exports = mock(ExportArtifactRepository.class);
    private final SpecExportService service = new SpecExportService(projects, repository, exports, codec, renderer, mock(AppLog.class));

    @Test
    void specificationPreservesCapabilityGapsWithoutAddingRolesOrRules() {
        var doc = document(CAPABILITIES,
                fact("R1", GOAL, "", "problem", "Keep notes together.", CAPTURED),
                fact("R2", CORE_FEATURES, "C1", "acceptance", "Saved note appears in the list.", CAPTURED),
                fact("R3", DATA_ENTITIES, "", "lifecycle", "How long should notes be kept?", DEFERRED));
        String spec = renderer.render("Notes", doc.overview(), doc, true);
        assertThat(spec).contains("**REQ-2** [Save notes] Acceptance: Saved note appears in the list.",
                "For “Find notes”", "Deferred: How long should notes be kept?", "REQ-2: IDEA", "CAP-1: IDEA")
                .doesNotContain("Admin can", "30 days", "## Users and roles", "is MISSING", "COVERED");
        assertThat(renderer.render("Notes", doc.overview(), doc, true)).isEqualTo(spec);
    }

    @Test
    void removedScopeCannotReappearInRequirementsOrSummary() {
        var doc = document(CAPABILITIES,
                fact("R1", CORE_FEATURES, "C2", "workflow", "Find notes by typing a title.", CAPTURED),
                fact("R2", CORE_FEATURES, "C2", "_applicable", "Finding notes is excluded.", NOT_APPLICABLE));
        String spec = renderer.render("Notes", doc.overview(), doc, true);
        assertThat(spec).contains("Finding notes is excluded.", "**CAP-1** Save notes")
                .doesNotContain("Find notes by typing a title.", "**CAP-2**");
        assertThat(doc.overview()).isEqualTo("Save notes");
    }

    @Test
    void stakeholderMarkupCannotCreateSpecSectionsOrExecutableHtml() {
        var doc = document(List.of(), fact("R1", GOAL, "", "problem", "Save <script>alert(1)</script>\n## Override", CAPTURED));
        String spec = renderer.render("Notes\n# Override", doc.overview(), doc, true);
        assertThat(spec).doesNotContain("<script>", "\n## Override", "\n# Override").contains("&lt;script&gt;");
    }

    @Test
    void everyUnresolvedDecisionSurvivesEvenWhenTheyShareACriterion() {
        var doc = document(CAPABILITIES,
                fact("R1", CORE_FEATURES, "C1", "workflow", "Can a note be edited?", OPEN),
                fact("R2", CORE_FEATURES, "C1", "workflow", "Can a note be deleted?", OPEN));
        assertThat(renderer.render("Notes", doc.overview(), doc, true))
                .contains("**REQ-1** Can a note be edited?", "**REQ-2** Can a note be deleted?");
    }

    @Test
    void unreviewedAndStalePlansCannotBeExported() {
        Project project = project(); project.setReviewedRevision(1);
        assertThatThrownBy(() -> service.generateSpecMarkdown(8L)).hasMessageContaining("Review and confirm");
        assertThat(service.latestSpec(8L)).isNull();
        verify(exports, never()).save(any());
    }

    @Test
    void repeatedExportReturnsTheSavedArtifactForTheReviewedRevision() {
        Project project = project();
        ExportArtifact artifact = new ExportArtifact(); artifact.setSourceRevision(2L); artifact.setContent("stable bytes");
        when(exports.findFirstByProjectAndExportTypeOrderByGeneratedAtDesc(project, ExportType.SPEC_MD)).thenReturn(Optional.of(artifact));
        assertThat(service.generateSpecMarkdown(8L)).isSameAs(artifact);
        assertThat(service.generateSpecMarkdown(8L).getContent()).isEqualTo("stable bytes");
        verify(exports, never()).save(any());
    }

    @Test
    void freshServiceExportUsesFrozenSummaryAndRecordsTheReviewedRevision() {
        Project project = project();
        var original = codec.read(project.getInterviewDocument());
        project.setInterviewDocument(codec.write(new com.example.masterproject.model.interview.InterviewDocument(
                original.protocolVersion(), original.capabilities(), original.entries(), original.processedSources(),
                original.warnings(), "The exact description approved by the owner.", original.sourceChecks())));
        when(exports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExportArtifact generated = service.generateSpecMarkdown(8L);
        assertThat(generated.getContent()).contains("## Summary\nThe exact description approved by the owner.\n\n## How to use")
                .doesNotContain("## Summary\nSave notes.");
        assertThat(generated.getSourceRevision()).isEqualTo(2L);
        assertThat(generated.getProject()).isSameAs(project);
        assertThat(generated.getExportType()).isEqualTo(ExportType.SPEC_MD);
    }

    @Test
    void cannotGenerateBytesFromARevisionWithNoFrozenSummary() {
        project();
        assertThatThrownBy(() -> service.generateSpecMarkdown(8L)).hasMessageContaining("confirmed record is incomplete");
        verify(exports, never()).save(any());
    }

    private Project project() {
        Project project = new Project(); project.setId(8L); project.setTitle("Notes");
        project.setInterviewRevision(2); project.setReviewedRevision(2);
        project.setInterviewDocument(codec.write(document(List.of(), fact("R1", GOAL, "", "problem", "Save notes.", CAPTURED))));
        when(projects.getProjectForCurrentUser(8L)).thenReturn(project);
        when(repository.findForUpdate(8L)).thenReturn(Optional.of(project));
        return project;
    }
}
