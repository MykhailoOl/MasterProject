package com.example.masterproject.service;

import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
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

    public SpecExportService(
            ProjectService projectService,
            ProjectCategoryRepository projectCategoryRepository,
            RequirementSlotRepository requirementSlotRepository,
            ExportArtifactRepository exportArtifactRepository) {
        this.projectService = projectService;
        this.projectCategoryRepository = projectCategoryRepository;
        this.requirementSlotRepository = requirementSlotRepository;
        this.exportArtifactRepository = exportArtifactRepository;
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

        for (ProjectCategory category : enabled) {
            TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(category.getCategory());
            if (!definition.includeInSpecBody()) {
                continue;
            }
            RequirementSlot slot = slots.get(category.getCategory());
            markdown.append("## ").append(definition.specHeading()).append("\n");
            if (slot == null || slot.getValue() == null || slot.getValue().isBlank()) {
                markdown.append("- Not specified\n\n");
                openDecisions.add(definition.displayName() + " was selected but not filled in.");
            } else {
                for (String line : slot.getValue().split("\\r?\\n|\\|")) {
                    String cleaned = line.trim();
                    if (!cleaned.isEmpty()) {
                        markdown.append("- ").append(cleaned).append("\n");
                    }
                }
                markdown.append("\n");
                if (slot.getCompleteness() < 0.7) {
                    openDecisions.add(definition.displayName() + " still looks incomplete.");
                }
            }
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
        markdown.append("- Ask before inventing requirements that contradict Open decisions.\n");
        markdown.append("- Keep changes scoped; do not expand into Non-goals if that section exists.\n");
        markdown.append("- Write clear code and tests that match Testing expectations when present.\n");

        ExportArtifact artifact = new ExportArtifact();
        artifact.setProject(project);
        artifact.setExportType(ExportType.SPEC_MD);
        artifact.setContent(markdown.toString());
        artifact.setGeneratedAt(Instant.now());
        return exportArtifactRepository.save(artifact);
    }

    @Transactional(readOnly = true)
    public ExportArtifact latestSpec(Long projectId) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        return exportArtifactRepository
                .findFirstByProjectAndExportTypeOrderByGeneratedAtDesc(project, ExportType.SPEC_MD)
                .orElse(null);
    }
}
