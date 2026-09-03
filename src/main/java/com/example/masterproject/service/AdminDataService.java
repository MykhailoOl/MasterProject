package com.example.masterproject.service;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.repository.AnswerRepository;
import com.example.masterproject.repository.CompletenessSnapshotRepository;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.QuestionRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import com.example.masterproject.repository.UserRepository;
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
            String displayName,
            String role,
            Instant createdAt,
            long projectCount) {
    }

    public record AdminProjectRow(
            Long id,
            Long ownerId,
            String ownerEmail,
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
            String displayName,
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
            Instant updatedAt) {
    }

    public record SessionExportRow(
            Long id,
            Long projectId,
            String conditionTag,
            Instant startedAt,
            Instant completedAt) {
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
            Instant createdAt) {
    }

    public record AnswerExportRow(
            Long id,
            Long questionId,
            String answerText,
            Instant answeredAt) {
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
            Instant generatedAt) {
    }

    public record StudyExport(
            Instant generatedAt,
            List<UserExportRow> users,
            List<ProjectExportRow> projects,
            List<SessionExportRow> sessions,
            List<QuestionExportRow> questions,
            List<AnswerExportRow> answers,
            List<SlotExportRow> slots,
            List<SnapshotExportRow> snapshots,
            List<ArtifactExportRow> exports) {
    }

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
            ObjectMapper objectMapper) {
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
                        user.getDisplayName(),
                        user.getRole().name(),
                        user.getCreatedAt(),
                        projectCounts.getOrDefault(user.getId(), 0L)))
                .toList();
        List<AdminProjectRow> projectRows = projects.stream()
                .map(project -> new AdminProjectRow(
                        project.getId(),
                        project.getOwner().getId(),
                        project.getOwner().getEmail(),
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
    public byte[] jsonExport() {
        userContextService.requireAdmin();
        try {
            return objectMapper.writeValueAsBytes(collectExport());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create JSON export", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] csvArchive() {
        userContextService.requireAdmin();
        StudyExport data = collectExport();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            addCsv(zip, "users.csv",
                    List.of("id", "email", "display_name", "role", "created_at"),
                    data.users().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.email(),
                                    nullable(row.displayName()),
                                    row.role(),
                                    row.createdAt()))
                            .toList());
            addCsv(zip, "projects.csv",
                    List.of("id", "owner_id", "title", "initial_idea", "status", "llm_provider",
                            "simplify_mode_enabled", "created_at", "updated_at"),
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
                                    row.updatedAt()))
                            .toList());
            addCsv(zip, "sessions.csv",
                    List.of("id", "project_id", "condition_tag", "started_at", "completed_at"),
                    data.sessions().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    row.conditionTag(),
                                    row.startedAt(),
                                    nullable(row.completedAt())))
                            .toList());
            addCsv(zip, "questions.csv",
                    List.of("id", "session_id", "category", "focus_criterion", "question_text",
                            "simplified_text", "options_json", "question_order", "created_at"),
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
                                    row.createdAt()))
                            .toList());
            addCsv(zip, "answers.csv",
                    List.of("id", "question_id", "answer_text", "answered_at"),
                    data.answers().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.questionId(),
                                    row.answerText(),
                                    row.answeredAt()))
                            .toList());
            addCsv(zip, "slots.csv",
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
            addCsv(zip, "snapshots.csv",
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
                    List.of("id", "project_id", "export_type", "content", "generated_at"),
                    data.exports().stream()
                            .map(row -> List.of(
                                    row.id(),
                                    row.projectId(),
                                    row.exportType(),
                                    row.content(),
                                    row.generatedAt()))
                            .toList());
            zip.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create CSV archive", ex);
        }
    }

    private StudyExport collectExport() {
        List<UserExportRow> users = userRepository.findAll(Sort.by("id")).stream()
                .map(user -> new UserExportRow(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole().name(),
                        user.getCreatedAt()))
                .toList();
        List<ProjectExportRow> projects = projectRepository.findAllByOrderByIdAsc().stream()
                .map(project -> new ProjectExportRow(
                        project.getId(),
                        project.getOwner().getId(),
                        project.getTitle(),
                        project.getInitialIdea(),
                        project.getStatus().name(),
                        project.getLlmProvider() == null ? null : project.getLlmProvider().name(),
                        project.isSimplifyModeEnabled(),
                        project.getCreatedAt(),
                        project.getUpdatedAt()))
                .toList();
        List<SessionExportRow> sessions = sessionRepository.findAll(Sort.by("id")).stream()
                .map(session -> new SessionExportRow(
                        session.getId(),
                        session.getProject().getId(),
                        session.getConditionTag().name(),
                        session.getStartedAt(),
                        session.getCompletedAt()))
                .toList();
        List<QuestionExportRow> questions = questionRepository.findAll(Sort.by("id")).stream()
                .map(question -> new QuestionExportRow(
                        question.getId(),
                        question.getSession().getId(),
                        question.getCategory().name(),
                        question.getFocusCriterion(),
                        question.getQuestionText(),
                        question.getSimplifiedText(),
                        question.getOptionsJson(),
                        question.getQuestionOrder(),
                        question.getCreatedAt()))
                .toList();
        List<AnswerExportRow> answers = answerRepository.findAll(Sort.by("id")).stream()
                .map(answer -> new AnswerExportRow(
                        answer.getId(),
                        answer.getQuestion().getId(),
                        answer.getAnswerText(),
                        answer.getAnsweredAt()))
                .toList();
        List<SlotExportRow> slots = slotRepository.findAll(Sort.by("id")).stream()
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
                .map(artifact -> new ArtifactExportRow(
                        artifact.getId(),
                        artifact.getProject().getId(),
                        artifact.getExportType().name(),
                        artifact.getContent(),
                        artifact.getGeneratedAt()))
                .toList();
        return new StudyExport(
                Instant.now(),
                users,
                projects,
                sessions,
                questions,
                answers,
                slots,
                snapshots,
                exports);
    }

    private JsonNode parseJson(String value) {
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
