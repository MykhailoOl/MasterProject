package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectCategoryRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpecExportServiceTests {

    @Test
    void openDecisionsIncludeEveryNonCoveredCriterion() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectCategoryRepository categoryRepository = mock(ProjectCategoryRepository.class);
        RequirementSlotRepository slotRepository = mock(RequirementSlotRepository.class);
        ExportArtifactRepository exportRepository = mock(ExportArtifactRepository.class);
        SpecEnrichmentService enrichmentService = mock(SpecEnrichmentService.class);
        AppLog appLog = mock(AppLog.class);
        GuidedElicitationPlanner planner = new GuidedElicitationPlanner(new ObjectMapper());
        SpecExportService service = new SpecExportService(
                projectService,
                categoryRepository,
                slotRepository,
                exportRepository,
                enrichmentService,
                planner,
                appLog);

        Project project = new Project();
        project.setId(8L);
        project.setTitle("Tracker");
        project.setInitialIdea("Track student assignments.");
        ProjectCategory goal = new ProjectCategory();
        goal.setProject(project);
        goal.setCategory(RequirementCategory.GOAL);
        RequirementSlot slot = new RequirementSlot();
        slot.setProject(project);
        slot.setCategory(RequirementCategory.GOAL);
        slot.setValue("Students miss deadlines | They want one tracking view.");
        slot.setCompleteness(0.75);
        slot.setAssessmentJson(
                """
                {"problem":"COVERED","outcome":"COVERED","success":"PARTIAL","priority":"MISSING"}
                """);

        when(projectService.getProjectForCurrentUser(8L)).thenReturn(project);
        when(categoryRepository.findByProjectOrderByIdAsc(project)).thenReturn(List.of(goal));
        when(slotRepository.findByProjectOrderByCategoryAsc(project)).thenReturn(List.of(slot));
        when(enrichmentService.enrichUsersAndRoles(any(), any())).thenReturn(List.of("Admin role"));
        when(exportRepository.save(any(ExportArtifact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExportArtifact artifact = service.generateSpecMarkdown(8L);

        assertThat(artifact.getContent()).contains("success is PARTIAL");
        assertThat(artifact.getContent()).contains("priority is MISSING");
        assertThat(artifact.getContent()).doesNotContain("problem is COVERED");
    }
}
