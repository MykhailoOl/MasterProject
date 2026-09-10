package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.enums.ExportType;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecExportService {
    private final ProjectService projects;
    private final ProjectRepository projectRepository;
    private final ExportArtifactRepository exports;
    private final InterviewDocumentCodec codec;
    private final SpecRenderer renderer;
    private final AppLog log;

    public SpecExportService(ProjectService projects, ProjectRepository projectRepository,
                             ExportArtifactRepository exports, InterviewDocumentCodec codec,
                             SpecRenderer renderer, AppLog log) {
        this.projects = projects; this.projectRepository = projectRepository;
        this.exports = exports; this.codec = codec; this.renderer = renderer; this.log = log;
    }

    @Transactional
    public ExportArtifact generateSpecMarkdown(Long projectId) {
        projects.getProjectForCurrentUser(projectId);
        Project project = projectRepository.findForUpdate(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        if (!project.isCurrentProtocol() || project.getReviewedRevision() < 0 || project.getReviewedRevision() != project.getInterviewRevision()) {
            throw new IllegalStateException("Review and confirm the current plan before creating its SPEC file.");
        }
        ExportArtifact existing = latestFor(project);
        if (existing != null) return existing;
        var document = codec.read(project.getInterviewDocument());
        if (document.summary().isBlank() || !document.hasCheckedSource("IDEA") || !document.warnings().isEmpty()) {
            throw new IllegalStateException("The confirmed record is incomplete. Review and confirm it again before export.");
        }
        ExportArtifact artifact = new ExportArtifact();
        artifact.setProject(project); artifact.setExportType(ExportType.SPEC_MD);
        artifact.setSourceRevision(project.getReviewedRevision());
        artifact.setContent(renderer.render(project.getTitle(), document.summary(), document, true));
        artifact.setGeneratedAt(Instant.now());
        log.info("SPEC", "project=" + projectId + " revision=" + project.getReviewedRevision() + " event=export");
        return exports.save(artifact);
    }

    @Transactional(readOnly = true)
    public ExportArtifact latestSpec(Long id) { return latestFor(projects.getProjectForCurrentUser(id)); }

    private ExportArtifact latestFor(Project project) {
        if (!project.isCurrentProtocol() || project.getReviewedRevision() < 0 || project.getReviewedRevision() != project.getInterviewRevision()) return null;
        return exports.findFirstByProjectAndExportTypeOrderByGeneratedAtDesc(project, ExportType.SPEC_MD)
                .filter(e -> e.getSourceRevision() != null && e.getSourceRevision() == project.getReviewedRevision())
                .orElse(null);
    }
}
