package com.example.masterproject.service;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.AnswerRepository;
import com.example.masterproject.repository.CompletenessSnapshotRepository;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.QuestionRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import com.example.masterproject.repository.UserRepository;
import com.example.masterproject.repository.InterviewRevisionRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AdminDataService {

    public record AdminUserRow(
            Long id,
            String email,
            String username,
            String role,
            Instant createdAt,
            long projectCount) {
    }

    public record AdminProjectRow(
            Long id,
            Long ownerId,
            String ownerEmail,
            String ownerUsername,
            String title,
            String status,
            String llmProvider,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DashboardData(
            List<AdminUserRow> users,
            List<AdminProjectRow> projects,
            long sessionCount,
            long questionCount,
            long answerCount,
            long slotCount,
            long snapshotCount,
            long exportCount) {
    }

    public record UserExportRow(
            Long id,
            String email,
            String username,
            String role,
            Instant createdAt) {
    }

    public record ProjectExportRow(
            Long id,
            Long ownerId,
            String title,
            String initialIdea,
            String status,
            String llmProvider,
            boolean simplifyModeEnabled,
            Instant createdAt,
            Instant updatedAt,
            long interviewRevision,
            long reviewedRevision,
            JsonNode interviewDocument,
            String collectionProtocol) {
    }

    public record SessionExportRow(
            Long id,
            Long projectId,
            String conditionTag,
            Instant startedAt,
            Instant completedAt,
            boolean studyEnrolled, String assignmentMethod, int questionBudget, String studyModel, String endReason) {
    }

    public record QuestionExportRow(
            Long id,
            Long sessionId,
            String category,
            String focusCriterion,
            String questionText,
            String simplifiedText,
            String optionsJson,
            int questionOrder,
            Instant createdAt,
            String questionKind,
            String focusCapability,
            String generationOrigin, String generationReason, String llmCallId, String promptVersion) {
    }

    public record AnswerExportRow(
            Long id,
            Long questionId,
            String answerText,
            Instant answeredAt, String provenance) {
    }

    public record SlotExportRow(
            Long id,
            Long projectId,
            String category,
            String value,
            String assessmentJson,
            double completeness,
            String source,
            Instant updatedAt) {
    }

    public record SnapshotExportRow(
            Long id,
            Long projectId,
            Long sessionId,
            Long answerId,
            String answeredCategory,
            Integer sequenceNumber,
            JsonNode scores,
            double totalScore,
            Instant capturedAt) {
    }

    public record ArtifactExportRow(
            Long id,
            Long projectId,
            String exportType,
            String content,
            Instant generatedAt,
            Long sourceRevision) {
    }

    public record RevisionExportRow(Long id, Long projectId, long revision, String eventType,
                                    JsonNode document, Instant createdAt) {}

    public record StudyExport(
            String datasetScope,
            String metricPurpose,
            List<ExcludedProject> excludedProjects,
            Instant generatedAt,
            List<UserExportRow> users,
            List<ProjectExportRow> projects,
            List<SessionExportRow> sessions,
            List<QuestionExportRow> questions,
            List<AnswerExportRow> answers,
            List<SlotExportRow> slots,
            List<SnapshotExportRow> snapshots,
            List<ArtifactExportRow> exports,
            List<RevisionExportRow> revisions,
            List<com.example.masterproject.model.entity.LlmCallAudit> llmCalls) {
    }
    public record ExcludedProject(Long projectId, String reason) {}

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ElicitationSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final RequirementSlotRepository slotRepository;
    private final CompletenessSnapshotRepository snapshotRepository;
    private final ExportArtifactRepository artifactRepository;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;
    private final InterviewRevisionRepository revisionRepository;
    private final com.example.masterproject.repository.LlmCallAuditRepository callRepository;

    public AdminDataService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ElicitationSessionRepository sessionRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            RequirementSlotRepository slotRepository,
            CompletenessSnapshotRepository snapshotRepository,
            ExportArtifactRepository artifactRepository,
            UserContextService userContextService,
            ObjectMapper objectMapper,
            InterviewRevisionRepository revisionRepository,
            com.example.masterproject.repository.LlmCallAuditRepository callRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.slotRepository = slotRepository;
        this.snapshotRepository = snapshotRepository;
        this.artifactRepository = artifactRepository;
        this.userContextService = userContextService;
        this.objectMapper = objectMapper;
        this.revisionRepository = revisionRepository;
        this.callRepository = callRepository;
    }

    @Transactional(readOnly = true)
    public DashboardData dashboard() {
        userContextService.requireAdmin();
        List<Project> projects = projectRepository.findAllByOrderByIdAsc();
        Map<Long, Long> projectCounts = projects.stream()
                .collect(Collectors.groupingBy(
                        project -> project.getOwner().getId(),
                        Collectors.counting()));
        List<AdminUserRow> users = userRepository.findAll(Sort.by("id")).stream()
                .map(user -> new AdminUserRow(
                        user.getId(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.getCreatedAt(),
                        projectCounts.getOrDefault(user.getId(), 0L)))
                .toList();
        List<AdminProjectRow> projectRows = projects.stream()
                .map(project -> new AdminProjectRow(
                        project.getId(),
                        project.getOwner().getId(),
                        project.getOwner().getEmail(),
                        project.getOwner().getUsername(),
                        project.getTitle(),
                        project.getStatus().name(),
                        project.getLlmProvider() == null ? null : project.getLlmProvider().name(),
                        project.getCreatedAt(),
                        project.getUpdatedAt()))
                .toList();
        return new DashboardData(
                users,
                projectRows,
                sessionRepository.count(),
                questionRepository.count(),
                answerRepository.count(),
                slotRepository.count(),
                snapshotRepository.count(),
                artifactRepository.count());
    }

    @Transactional(readOnly = true)
    public AdminUserDetail userDetail(Long userId) {
        userContextService.requireAdmin();
        var user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<AdminProjectRow> projects = projectRepository.findByOwnerOrderByUpdatedAtDesc(user).stream()
                .map(project -> new AdminProjectRow(
                        project.getId(),
                        project.getOwner().getId(),
                        project.getOwner().getEmail(),
                        project.getOwner().getUsername(),
                        project.getTitle(),
                        project.getStatus().name(),
                        project.getLlmProvider() == null ? null : project.getLlmProvider().name(),
                        project.getCreatedAt(),
                        project.getUpdatedAt()))
                .toList();
        return new AdminUserDetail(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.getCreatedAt(),
                projects);
    }

    @Transactional(readOnly = true)
    public AdminProjectDetail projectDetail(Long projectId) {
        userContextService.requireAdmin();
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        List<AdminSlotRow> slots = slotRepository.findByProjectOrderByCategoryAsc(project).stream()
                .map(slot -> new AdminSlotRow(
                        TaxonomyCatalog.require(slot.getCategory()).displayName(),
                        slot.getValue(),
                        slot.getCompleteness(),
                        slot.getSource().name(),
                        slot.getUpdatedAt()))
                .toList();
        var sessions = sessionRepository.findByProjectOrderByStartedAtDesc(project);
        long questionCount = sessions.stream()
                .mapToLong(session -> questionRepository.countBySession(session))
                .sum();
        long answerCount = sessions.stream()
                .flatMap(session -> questionRepository.findBySessionOrderByQuestionOrderAsc(session).stream())
                .filter(question -> answerRepository.existsByQuestion(question))
                .count();
        return new AdminProjectDetail(
                project.getId(),
                project.getTitle(),
                project.getInitialIdea(),
                project.getStatus().name(),
                project.getLlmProvider() == null ? null : project.getLlmProvider().name(),
                project.isSimplifyModeEnabled(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getOwner().getId(),
                project.getOwner().getEmail(),
                project.getOwner().getUsername(),
                sessions.size(),
                questionCount,
                answerCount,
                slots);
    }

    public record AdminUserDetail(
            Long id,
            String email,
            String username,
            String role,
            Instant createdAt,
            List<AdminProjectRow> projects) {
    }

    public record AdminSlotRow(
            String category,
            String value,
            double completeness,
            String source,
            Instant updatedAt) {
    }

    public record AdminProjectDetail(
            Long id,
            String title,
            String initialIdea,
            String status,
            String llmProvider,
            boolean simplifyModeEnabled,
            Instant createdAt,
            Instant updatedAt,
            Long ownerId,
            String ownerEmail,
            String ownerUsername,
            int sessionCount,
            long questionCount,
            long answerCount,
            List<AdminSlotRow> slots) {
    }

    @Transactional(readOnly = true)
    public byte[] jsonExport() {
        return jsonExport(false);
    }

    @Transactional(readOnly = true)
    public byte[] jsonExport(boolean includeExcluded) {
        userContextService.requireAdmin();
        try {
            return objectMapper.writeValueAsBytes(collectExport(includeExcluded));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create JSON export", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] csvArchive() {
        return csvArchive(false);
    }

    @Transactional(readOnly = true)
    public byte[] csvArchive(boolean includeExcluded) {
        userContextService.requireAdmin();
        StudyExport data = collectExport(includeExcluded);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(objectMapper.writeValueAsBytes(Map.of("datasetScope", data.datasetScope(),
                    "metricPurpose", data.metricPurpose(), "excludedProjects", data.excludedProjects())));
            zip.closeEntry();
            addCsv(zip, "users.csv",
                    List.of("id", "email", "username", "role", "created_at"),
                    data.users().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.email(),
                                    row.username(),
                                    row.role(),
                                    row.createdAt()))
                            .toList());
            addCsv(zip, "projects.csv",
                    List.of("id", "owner_id", "title", "initial_idea", "status", "llm_provider",
                            "simplify_mode_enabled", "created_at", "updated_at", "interview_revision", "reviewed_revision", "interview_document", "collection_protocol"),
                    data.projects().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.ownerId(),
                                    row.title(),
                                    row.initialIdea(),
                                    row.status(),
                                    nullable(row.llmProvider()),
                                    row.simplifyModeEnabled(),
                                    row.createdAt(),
                                    row.updatedAt(), row.interviewRevision(), row.reviewedRevision(), nullable(row.interviewDocument()), row.collectionProtocol()))
                            .toList());
            addCsv(zip, "sessions.csv",
                    List.of("id", "project_id", "condition_tag", "started_at", "completed_at", "study_enrolled", "assignment_method", "question_budget", "study_model", "end_reason"),
                    data.sessions().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    row.conditionTag(),
                                    row.startedAt(),
                                    nullable(row.completedAt()), row.studyEnrolled(), row.assignmentMethod(), row.questionBudget(), nullable(row.studyModel()), nullable(row.endReason())))
                            .toList());
            addCsv(zip, "questions.csv",
                    List.of("id", "session_id", "category", "focus_criterion", "question_text",
                            "simplified_text", "options_json", "question_order", "created_at", "question_kind", "focus_capability", "generation_origin", "generation_reason", "llm_call_id", "prompt_version"),
                    data.questions().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.sessionId(),
                                    row.category(),
                                    nullable(row.focusCriterion()),
                                    row.questionText(),
                                    nullable(row.simplifiedText()),
                                    nullable(row.optionsJson()),
                                    row.questionOrder(),
                                    row.createdAt(), row.questionKind(), nullable(row.focusCapability()), row.generationOrigin(), nullable(row.generationReason()), nullable(row.llmCallId()), nullable(row.promptVersion())))
                            .toList());
            addCsv(zip, "answers.csv",
                    List.of("id", "question_id", "answer_text", "answered_at", "provenance"),
                    data.answers().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.questionId(),
                                    row.answerText(),
                                    row.answeredAt(), row.provenance()))
                            .toList());
            addCsv(zip, "diagnostic-slots.csv",
                    List.of("id", "project_id", "category", "value", "assessment_json",
                            "completeness", "source", "updated_at"),
                    data.slots().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    row.category(),
                                    nullable(row.value()),
                                    nullable(row.assessmentJson()),
                                    row.completeness(),
                                    row.source(),
                                    row.updatedAt()))
                            .toList());
            addCsv(zip, "diagnostic-snapshots.csv",
                    List.of("id", "project_id", "session_id", "answer_id", "answered_category",
                            "sequence_number", "scores_json", "total_score", "captured_at"),
                    data.snapshots().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    nullable(row.sessionId()),
                                    nullable(row.answerId()),
                                    nullable(row.answeredCategory()),
                                    nullable(row.sequenceNumber()),
                                    row.scores().toString(),
                                    row.totalScore(),
                                    row.capturedAt()))
                            .toList());
            addCsv(zip, "exports.csv",
                    List.of("id", "project_id", "export_type", "content", "generated_at", "source_revision"),
                    data.exports().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    row.exportType(),
                                    row.content(),
                                    row.generatedAt(), nullable(row.sourceRevision())))
                            .toList());
            addCsv(zip, "revisions.csv", List.of("id", "project_id", "revision", "event_type", "document", "created_at"),
                    data.revisions().stream().map(row -> List.of(row.id(), row.projectId(), row.revision(),
                            row.eventType(), row.document().toString(), row.createdAt())).toList());
            addCsv(zip, "llm-calls.csv", List.of("id", "project_id", "phase", "prompt_version", "provider", "outcome",
                            "requested_temperature", "duration_ms", "metadata_json", "created_at"),
                    data.llmCalls().stream().map(row -> List.of(row.getId(), row.getProjectId(), row.getPhase(),
                            row.getPromptVersion(), row.getProvider(), row.getOutcome(), row.getRequestedTemperature(),
                            row.getDurationMs(), row.getMetadataJson(), row.getCreatedAt())).toList());
            zip.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create CSV archive", ex);
        }
    }

    private StudyExport collectExport(boolean includeExcluded) {
        var allProjects = projectRepository.findAllByOrderByIdAsc();
        var allSessions = sessionRepository.findAll(Sort.by("id"));
        var enrolledIds = allSessions.stream().filter(s -> s.isStudyEnrolled() && "RANDOMIZED".equals(s.getAssignmentMethod()))
                .map(s -> s.getProject().getId()).collect(Collectors.toSet());
        var excluded = allProjects.stream().filter(p -> !p.isCurrentProtocol() || !enrolledIds.contains(p.getId()))
                .map(p -> new ExcludedProject(p.getId(), !p.isCurrentProtocol() ? "LEGACY_COLLECTION_PROTOCOL" : "NOT_RANDOMIZED_STUDY_SESSION")).toList();
        var selected = allProjects.stream().filter(p -> includeExcluded || (p.isCurrentProtocol() && enrolledIds.contains(p.getId()))).toList();
        var projectIds = selected.stream().map(Project::getId).collect(Collectors.toSet());
        var userIds = selected.stream().map(p -> p.getOwner().getId()).collect(Collectors.toSet());
        List<UserExportRow> users = userRepository.findAll(Sort.by("id")).stream()
                .filter(user -> includeExcluded || userIds.contains(user.getId()))
                .map(user -> new UserExportRow(
                        user.getId(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.getCreatedAt()))
                .toList();
        List<ProjectExportRow> projects = selected.stream()
                .map(project -> new ProjectExportRow(
                        project.getId(),
                        project.getOwner().getId(),
                        project.getTitle(),
                        project.getInitialIdea(),
                        project.getStatus().name(),
                        project.getLlmProvider() == null ? null : project.getLlmProvider().name(),
                        project.isSimplifyModeEnabled(),
                        project.getCreatedAt(),
                        project.getUpdatedAt(), project.getInterviewRevision(), project.getReviewedRevision(),
                        parseJson(project.getInterviewDocument()), project.getCollectionProtocol()))
                .toList();
        List<SessionExportRow> sessions = allSessions.stream().filter(s -> projectIds.contains(s.getProject().getId()))
                .map(session -> new SessionExportRow(
                        session.getId(),
                        session.getProject().getId(),
                        session.getConditionTag().name(),
                        session.getStartedAt(),
                        session.getCompletedAt(), session.isStudyEnrolled(), session.getAssignmentMethod(), session.getQuestionBudget(), session.getStudyModel(), session.getEndReason()))
                .toList();
        List<QuestionExportRow> questions = questionRepository.findAll(Sort.by("id")).stream()
                .filter(q -> projectIds.contains(q.getSession().getProject().getId()))
                .map(question -> new QuestionExportRow(
                        question.getId(),
                        question.getSession().getId(),
                        question.getCategory().name(),
                        question.getFocusCriterion(),
                        question.getQuestionText(),
                        question.getSimplifiedText(),
                        question.getOptionsJson(),
                        question.getQuestionOrder(),
                        question.getCreatedAt(), question.getQuestionKind(), question.getFocusCapability(), question.getGenerationOrigin(), question.getGenerationReason(), question.getLlmCallId(), question.getPromptVersion()))
                .toList();
        List<AnswerExportRow> answers = answerRepository.findAll(Sort.by("id")).stream()
                .filter(a -> projectIds.contains(a.getQuestion().getSession().getProject().getId()))
                .map(answer -> new AnswerExportRow(
                        answer.getId(),
                        answer.getQuestion().getId(),
                        answer.getAnswerText(),
                        answer.getAnsweredAt(), answer.getProvenance()))
                .toList();
        List<SlotExportRow> slots = slotRepository.findAll(Sort.by("id")).stream()
                .filter(s -> projectIds.contains(s.getProject().getId()))
                .map(slot -> new SlotExportRow(
                        slot.getId(),
                        slot.getProject().getId(),
                        slot.getCategory().name(),
                        slot.getValue(),
                        slot.getAssessmentJson(),
                        slot.getCompleteness(),
                        slot.getSource().name(),
                        slot.getUpdatedAt()))
                .toList();
        List<SnapshotExportRow> snapshots = snapshotRepository.findAll(Sort.by("id")).stream()
                .filter(s -> projectIds.contains(s.getProject().getId()))
                .map(snapshot -> new SnapshotExportRow(
                        snapshot.getId(),
                        snapshot.getProject().getId(),
                        snapshot.getSession() == null ? null : snapshot.getSession().getId(),
                        snapshot.getAnswer() == null ? null : snapshot.getAnswer().getId(),
                        snapshot.getAnsweredCategory() == null ? null : snapshot.getAnsweredCategory().name(),
                        snapshot.getSequenceNumber() == 0 ? null : snapshot.getSequenceNumber(),
                        parseJson(snapshot.getScoresJson()),
                        snapshot.getTotalScore(),
                        snapshot.getCapturedAt()))
                .toList();
        List<ArtifactExportRow> exports = artifactRepository.findAll(Sort.by("id")).stream()
                .filter(a -> projectIds.contains(a.getProject().getId()))
                .map(artifact -> new ArtifactExportRow(
                        artifact.getId(),
                        artifact.getProject().getId(),
                        artifact.getExportType().name(),
                        artifact.getContent(),
                        artifact.getGeneratedAt(), artifact.getSourceRevision()))
                .toList();
        return new StudyExport(
                includeExcluded ? "ALL_DATA_DIAGNOSTIC" : "CURRENT_PROTOCOL_RANDOMIZED_INTENTION_TO_TREAT",
                "Taxonomy coverage in slots and snapshots is diagnostic, not an outcome measure of correctness or usability.",
                excluded,
                Instant.now(),
                users,
                projects,
                sessions,
                questions,
                answers,
                slots,
                snapshots,
                exports,
                revisionRepository.findAll(Sort.by("id")).stream().filter(r -> projectIds.contains(r.getProjectId())).map(revision -> new RevisionExportRow(
                        revision.getId(), revision.getProjectId(), revision.getRevision(), revision.getEventType(),
                        parseJson(revision.getDocumentJson()), revision.getCreatedAt())).toList(),
                callRepository.findAll(Sort.by("createdAt")).stream().filter(c -> projectIds.contains(c.getProjectId())).toList());
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) return objectMapper.getNodeFactory().nullNode();
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(value);
        }
    }

    private void addCsv(
            ZipOutputStream zip,
            String fileName,
            List<String> headers,
            List<? extends List<?>> rows) throws IOException {
        StringBuilder content = new StringBuilder();
        writeCsvRow(content, headers);
        for (List<?> row : rows) {
            writeCsvRow(content, row);
        }
        zip.putNextEntry(new ZipEntry(fileName));
        zip.write(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeCsvRow(StringBuilder target, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append(csvCell(values.get(index)));
        }
        target.append("\r\n");
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : value.toString();
        int firstContent = 0;
        while (firstContent < text.length() && Character.isWhitespace(text.charAt(firstContent))) {
            firstContent++;
        }
        boolean controlPrefix = !text.isEmpty()
                && (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\0');
        if (controlPrefix
                || (firstContent < text.length()
                        && "=+-@".indexOf(text.charAt(firstContent)) >= 0)) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }
}
