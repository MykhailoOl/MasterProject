package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.CriterionStatus;
import com.example.masterproject.model.enums.ExportType;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectCategoryRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecExportService {

    private final ProjectService projectService;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final RequirementSlotRepository requirementSlotRepository;
    private final ExportArtifactRepository exportArtifactRepository;
    private final SpecEnrichmentService specEnrichmentService;
    private final GuidedElicitationPlanner guidedElicitationPlanner;
    private final AppLog appLog;

    public SpecExportService(
            ProjectService projectService,
            ProjectCategoryRepository projectCategoryRepository,
            RequirementSlotRepository requirementSlotRepository,
            ExportArtifactRepository exportArtifactRepository,
            SpecEnrichmentService specEnrichmentService,
            GuidedElicitationPlanner guidedElicitationPlanner,
            AppLog appLog) {
        this.projectService = projectService;
        this.projectCategoryRepository = projectCategoryRepository;
        this.requirementSlotRepository = requirementSlotRepository;
        this.exportArtifactRepository = exportArtifactRepository;
        this.specEnrichmentService = specEnrichmentService;
        this.guidedElicitationPlanner = guidedElicitationPlanner;
        this.appLog = appLog;
    }

    @Transactional
    public ExportArtifact generateSpecMarkdown(Long projectId) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        List<ProjectCategory> enabled = projectCategoryRepository.findByProjectOrderByIdAsc(project);
        Map<RequirementCategory, RequirementSlot> slots =
                requirementSlotRepository.findByProjectOrderByCategoryAsc(project).stream()
                        .collect(Collectors.toMap(RequirementSlot::getCategory, slot -> slot));

        RequirementSlot titleSlot = slots.get(RequirementCategory.PROJECT_TITLE);
        String title = titleSlot != null && titleSlot.getValue() != null && !titleSlot.getValue().isBlank()
                ? titleSlot.getValue().trim()
                : project.getTitle();

        RequirementSlot overallSlot = slots.get(RequirementCategory.OVERALL_IDEA);
        String summary = overallSlot != null && overallSlot.getValue() != null && !overallSlot.getValue().isBlank()
                ? overallSlot.getValue().trim()
                : "Overall idea not confirmed yet.";

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Spec: ").append(title).append("\n\n");
        markdown.append("This document is a coding-oriented specification for an AI coding agent ")
                .append("(Cursor-friendly: keep sections short, concrete, and actionable).\n\n");
        markdown.append("## Summary\n");
        markdown.append(summary).append("\n\n");

        List<String> openDecisions = new ArrayList<>();
        RequirementSlot usersSlot = slots.get(RequirementCategory.USERS_AND_ROLES);
        String usersText = usersSlot == null || usersSlot.getValue() == null ? "" : usersSlot.getValue();

        for (ProjectCategory category : enabled) {
            TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(category.getCategory());
            if (!definition.includeInSpecBody()) {
                continue;
            }
            RequirementSlot slot = slots.get(category.getCategory());
            markdown.append("## ").append(definition.specHeading()).append("\n");
            if (slot == null || slot.getValue() == null || slot.getValue().isBlank()) {
                markdown.append("- Not specified\n");
                openDecisions.add(definition.displayName() + " was selected but not filled in.");
                appendCriterionOpenDecisions(openDecisions, definition, slot);
            } else {
                for (String line : slot.getValue().split("\\r?\\n|\\|")) {
                    String cleaned = line.trim();
                    if (!cleaned.isEmpty()) {
                        markdown.append("- ").append(cleaned).append("\n");
                    }
                }
                appendCriterionOpenDecisions(openDecisions, definition, slot);
            }

            if (category.getCategory() == RequirementCategory.USERS_AND_ROLES) {
                markdown.append("\n### Implementation roles for coding\n");
                markdown.append("These roles turn everyday stakeholder wording into settings the product needs:\n");
                for (String role : specEnrichmentService.enrichUsersAndRoles(project, usersText)) {
                    markdown.append("- ").append(role).append("\n");
                }
            }

            if (category.getCategory() == RequirementCategory.AUTHENTICATION) {
                String authText = slot == null || slot.getValue() == null ? "" : slot.getValue();
                markdown.append("\n### Access defaults for coding\n");
                for (String note : specEnrichmentService.enrichAuthentication(project, authText, usersText)) {
                    markdown.append("- ").append(note).append("\n");
                }
            }

            markdown.append("\n");
        }

        if (enabled.stream().noneMatch(row -> row.getCategory() == RequirementCategory.USERS_AND_ROLES)) {
            markdown.append("## Users and roles\n");
            markdown.append("- Not captured during elicitation.\n\n");
            markdown.append("### Implementation roles for coding\n");
            for (String role : specEnrichmentService.enrichUsersAndRoles(project, "")) {
                markdown.append("- ").append(role).append("\n");
            }
            markdown.append("\n");
        }

        markdown.append("## Open decisions / unknowns\n");
        if (openDecisions.isEmpty()) {
            markdown.append("- None recorded.\n\n");
        } else {
            for (String item : openDecisions) {
                markdown.append("- ").append(item).append("\n");
            }
            markdown.append("\n");
        }

        markdown.append("## Agent working notes\n");
        markdown.append("- Prefer implementing only what is listed under Core features / Goals.\n");
        markdown.append("- Implement the Implementation roles for coding even when the stakeholder used everyday words.\n");
        markdown.append("- Ask before inventing requirements that contradict Open decisions.\n");
        markdown.append("- Keep changes scoped; do not expand into Non-goals if that section exists.\n");
        markdown.append("- Write clear code and tests that match Testing expectations when present.\n");

        ExportArtifact artifact = new ExportArtifact();
        artifact.setProject(project);
        artifact.setExportType(ExportType.SPEC_MD);
        artifact.setContent(markdown.toString());
        artifact.setGeneratedAt(Instant.now());
        ExportArtifact saved = exportArtifactRepository.save(artifact);
        appLog.info("SPEC", "SPEC.md generated for project #" + project.getId() + ".");
        return saved;
    }

    private void appendCriterionOpenDecisions(
            List<String> openDecisions, TaxonomyCatalog.Definition definition, RequirementSlot slot) {
        String assessmentJson = slot == null ? null : slot.getAssessmentJson();
        Map<String, CriterionStatus> statuses = guidedElicitationPlanner.statuses(definition, assessmentJson);
        for (TaxonomyCatalog.Criterion criterion : definition.criteria()) {
            CriterionStatus status = statuses.getOrDefault(criterion.id(), CriterionStatus.MISSING);
            if (status != CriterionStatus.COVERED) {
                openDecisions.add(
                        definition.displayName() + " / " + criterion.id() + " is " + status.name() + ".");
            }
        }
    }

    @Transactional(readOnly = true)
    public ExportArtifact latestSpec(Long projectId) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        return exportArtifactRepository
                .findFirstByProjectAndExportTypeOrderByGeneratedAtDesc(project, ExportType.SPEC_MD)
                .orElse(null);
    }
}
