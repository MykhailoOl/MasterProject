package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.*;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewStore {
    public record Snapshot(Project project, ElicitationSession session, List<Question> questions,
                           List<Answer> answers, InterviewDocument document) {
        public List<Source> sources() {
            List<Source> result = new ArrayList<>();
            result.add(new Source("IDEA", "", project.getInitialIdea(), project.isCurrentProtocol() ? "STAKEHOLDER" : "LEGACY_UNKNOWN"));
            answers.forEach(a -> result.add(new Source("A" + a.getId(), a.getQuestion().getQuestionText(), a.getAnswerText(), a.getProvenance())));
            return List.copyOf(result);
        }
        public List<Focus> asked() {
            return answers.stream().map(Answer::getQuestion)
                    .filter(q -> q.getQuestionKind().equals("INTERVIEW"))
                    .filter(q -> !TaxonomyCatalog.isClosing(q.getCategory()))
                    .map(q -> new Focus(q.getCategory(), q.getFocusCapability(), q.getFocusCriterion(),
                            q.getQuestionText(), InterviewPlanner.help(), 0)).toList();
        }
        public Question unanswered() {
            Set<Long> answered = answers.stream().map(a -> a.getQuestion().getId())
                    .collect(java.util.stream.Collectors.toSet());
            return questions.stream().filter(q -> q.getQuestionKind().equals("INTERVIEW"))
                    .filter(q -> !answered.contains(q.getId())).findFirst().orElse(null);
        }
        public boolean assessed() {
            return project.isCurrentProtocol() && answers.stream().allMatch(a -> "STAKEHOLDER".equals(a.getProvenance()))
                    && document.warnings().isEmpty()
                    && sources().stream().allMatch(s -> document.hasCheckedSource(s.id()));
        }
    }

    private final ProjectRepository projects;
    private final ElicitationSessionRepository sessions;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final RequirementSlotRepository slots;
    private final InterviewRevisionRepository revisions;
    private final CompletenessSnapshotRepository snapshots;
    private final CompletenessSnapshotService snapshotService;
    private final UserContextService users;
    private final InterviewDocumentCodec codec;
    private final InterviewPlanner planner;
    private final AppLog log;

    public InterviewStore(ProjectRepository projects, ElicitationSessionRepository sessions,
                          QuestionRepository questions, AnswerRepository answers, RequirementSlotRepository slots,
                          InterviewRevisionRepository revisions, CompletenessSnapshotRepository snapshots,
                          CompletenessSnapshotService snapshotService, UserContextService users,
                          InterviewDocumentCodec codec, InterviewPlanner planner, AppLog log) {
        this.projects = projects; this.sessions = sessions; this.questions = questions; this.answers = answers;
        this.slots = slots; this.revisions = revisions; this.snapshots = snapshots;
        this.snapshotService = snapshotService; this.users = users; this.codec = codec; this.planner = planner; this.log = log;
    }

    @Transactional(readOnly = true)
    public Snapshot load(Long projectId) {
        Project project = owned(projects.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId)));
        ElicitationSession session = session(project);
        return new Snapshot(project, session, questions.findBySessionOrderByQuestionOrderAsc(session),
                answers.findByQuestionSessionOrderByQuestionQuestionOrderAsc(session), codec.read(project.getInterviewDocument()));
    }

    @Transactional
    public void saveDocument(Long id, long expectedRevision, InterviewDocument document) {
        Project project = lock(id, expectedRevision);
        requireCurrentProtocol(project);
        revise(project, document, "ASSESSMENT");
        for (RequirementSlot slot : slots.findByProjectOrderByCategoryAsc(project)) {
            if (TaxonomyCatalog.isClosing(slot.getCategory())) continue;
            slot.setCompleteness(planner.coverage(document, slot.getCategory()));
            slot.setValue(document.entries().stream().filter(e -> e.category() == slot.getCategory())
                    .filter(document::inScope)
                    .filter(e -> e.resolution() == Resolution.CAPTURED)
                    .map(Entry::text).collect(java.util.stream.Collectors.joining("\n")));
            slot.setAssessmentJson(codec.write(document.entries().stream()
                    .filter(e -> e.category() == slot.getCategory()).toList()));
            slot.setSource(RequirementSource.USER);
            slot.setUpdatedAt(Instant.now());
        }
        ElicitationSession session = session(project);
        List<Answer> completed = answers.findByQuestionSessionOrderByQuestionQuestionOrderAsc(session);
        if (document.warnings().isEmpty() && document.hasCheckedSource("IDEA")
                && completed.stream().allMatch(a -> document.hasCheckedSource("A" + a.getId()))
                && !completed.isEmpty()) {
            Answer latest = completed.getLast();
            if (!snapshots.existsByAnswer(latest)) snapshotService.captureAfterAnswer(project, session, latest);
        }
    }

    @Transactional
    public Question saveQuestion(Long id, long expectedRevision, Focus focus, String text) {
        return saveQuestion(id, expectedRevision, focus, text, "LOCAL", "unspecified", null);
    }

    @Transactional
    public Question saveQuestion(Long id, long expectedRevision, Focus focus, String text,
                                 String origin, String reason, String callId) {
        Project project = lock(id, expectedRevision);
        requireCurrentProtocol(project);
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.REVIEW) {
            throw new IllegalStateException("This interview is ready for review.");
        }
        ElicitationSession session = session(project);
        List<Question> existing = questions.findBySessionOrderByQuestionOrderAsc(session);
        Set<Long> answered = answers.findByQuestionSessionOrderByQuestionQuestionOrderAsc(session).stream()
                .map(a -> a.getQuestion().getId()).collect(java.util.stream.Collectors.toSet());
        Optional<Question> pending = existing.stream().filter(q -> q.getQuestionKind().equals("INTERVIEW"))
                .filter(q -> !answered.contains(q.getId())).findFirst();
        if (pending.isPresent()) return pending.get();
        Question question = new Question();
        question.setSession(session);
        question.setCategory(focus.category());
        question.setFocusCriterion(focus.criterion());
        question.setFocusCapability(focus.capabilityId());
        question.setQuestionText(text);
        question.setGenerationOrigin(origin);
        question.setGenerationReason(reason);
        question.setLlmCallId(callId);
        question.setPromptVersion(ElicitationService.PROMPT_VERSION);
        question.setQuestionOrder(existing.stream().mapToInt(Question::getQuestionOrder).max().orElse(0) + 1);
        log.info("INTERVIEW", "project=" + id + " event=question focus=" + focus.key()
                + " sequence=" + question.getQuestionOrder() + " condition=" + session.getConditionTag()
                + " origin=" + origin + " reason=" + reason + " call=" + callId);
        return questions.save(question);
    }

    @Transactional
    public void answer(Long id, Long questionId, String text) {
        Project project = lock(id, null);
        requireCurrentProtocol(project);
        validateText(text, 1, 5000, "Answer");
        ElicitationSession session = session(project);
        Question question = questions.findFirstBySessionAndId(session, questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        Optional<Answer> existing = answers.findByQuestion(question);
        if (existing.isPresent()) {
            if (existing.get().getAnswerText().equals(text.trim())) return;
            throw new IllegalStateException("This question was already answered. Use the review page to make a correction.");
        }
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.REVIEW) {
            throw new IllegalStateException("The interview has ended. Use the review page to make a correction.");
        }
        if (question.getCategory() == RequirementCategory.PROJECT_TITLE) validateText(text, 3, 255, "Project name");
        Answer answer = new Answer();
        answer.setQuestion(question); answer.setAnswerText(text.trim()); answers.save(answer);
        revise(project, codec.read(project.getInterviewDocument()), "ANSWER:A" + answer.getId());
        log.info("INTERVIEW", "project=" + id + " event=answer_saved answer=" + answer.getId());
    }

    @Transactional
    public void correct(Long id, long expectedRevision, String correction) {
        Project project = lock(id, expectedRevision);
        requireCurrentProtocol(project);
        validateText(correction, 3, 5000, "Correction");
        ElicitationSession session = session(project);
        Question question = new Question();
        question.setSession(session); question.setQuestionKind("REVIEW");
        question.setCategory(RequirementCategory.CORE_FEATURES); question.setFocusCriterion("review");
        question.setQuestionText("What should be corrected or added to the recorded requirements?");
        question.setQuestionOrder(questions.findBySessionOrderByQuestionOrderAsc(session).stream()
                .mapToInt(Question::getQuestionOrder).max().orElse(0) + 1);
        questions.save(question);
        Answer answer = new Answer();
        answer.setQuestion(question); answer.setAnswerText(correction); answers.save(answer);
        project.setStatus(ProjectStatus.REVIEW);
        session.setCompletedAt(null);
        revise(project, codec.read(project.getInterviewDocument()), "CORRECTION:A" + answer.getId());
    }

    @Transactional
    public void finish(Long id, long expectedRevision) {
        finish(id, expectedRevision, "PARTICIPANT");
    }

    @Transactional
    public void finish(Long id, long expectedRevision, String reason) {
        Project project = lock(id, expectedRevision);
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.REVIEW) return;
        project.setStatus(ProjectStatus.REVIEW);
        session(project).setEndReason(reason);
        revise(project, codec.read(project.getInterviewDocument()), "INTERVIEW_ENDED");
    }

    @Transactional
    public void confirm(Long id, long expectedRevision, String title, boolean accepted) {
        Project project = lock(id, expectedRevision);
        requireCurrentProtocol(project);
        if (!accepted) throw new IllegalArgumentException("Please read the plan and confirm it represents what you mean.");
        if (project.getStatus() != ProjectStatus.REVIEW && project.getStatus() != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Finish the interview before confirming your plan.");
        }
        validateText(title, 3, 255, "Project name");
        InterviewDocument document = codec.read(project.getInterviewDocument());
        ElicitationSession session = session(project);
        List<Answer> allAnswers = answers.findByQuestionSessionOrderByQuestionQuestionOrderAsc(session);
        if (allAnswers.stream().anyMatch(a -> !"STAKEHOLDER".equals(a.getProvenance()))) {
            throw new IllegalStateException("An answer has unverified provenance. Start a new interview using your own words.");
        }
        if (!document.hasCheckedSource("IDEA") || !document.warnings().isEmpty()
                || allAnswers.stream().anyMatch(a -> !document.hasCheckedSource("A" + a.getId()))) {
            throw new IllegalStateException("Some answers still need checking. Retry the check before confirming.");
        }
        if (document.activeCapabilities().isEmpty() && document.entries().stream().filter(document::inScope)
                .noneMatch(e -> e.resolution() == Resolution.CAPTURED && e.category() != RequirementCategory.NON_GOALS)) {
            throw new IllegalStateException("Add at least one concrete need using the correction box before confirming.");
        }
        project.setTitle(title.trim());
        InterviewDocument reviewed = new InterviewDocument(document.protocolVersion(), document.capabilities(),
                document.entries(), document.processedSources(), document.warnings(), document.overview(), document.sourceChecks(), document.claimChecks());
        revise(project, reviewed, "REVIEW_CONFIRMED");
        project.setReviewedRevision(project.getInterviewRevision());
        project.setStatus(ProjectStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        log.info("REVIEW", "project=" + id + " event=confirmed revision=" + project.getReviewedRevision());
    }

    private Project owned(Project project) {
        if (!project.getOwner().getId().equals(users.getCurrentUser().getId())) {
            throw new ProjectAccessDeniedException(project.getId());
        }
        return project;
    }
    private void requireCurrentProtocol(Project project) {
        if (!project.isCurrentProtocol()) throw new IllegalStateException(
                "This interview used an older collection method. Its answers are preserved, but cannot be reused as verified evidence. Start a new project using your own words.");
    }
    private Project lock(Long id, Long revision) {
        Project project = owned(projects.findForUpdate(id).orElseThrow(() -> new ProjectNotFoundException(id)));
        if (revision != null && project.getInterviewRevision() != revision) {
            throw new IllegalStateException("The project changed in another request. Reload to use the latest saved version.");
        }
        return project;
    }
    private ElicitationSession session(Project project) {
        return sessions.findFirstByProjectOrderByStartedAtDesc(project)
                .orElseThrow(() -> new IllegalStateException("Interview session missing"));
    }
    private void revise(Project project, InterviewDocument document, String event) {
        project.setInterviewDocument(codec.write(document));
        project.setInterviewRevision(project.getInterviewRevision() + 1);
        project.setReviewedRevision(-1);
        project.setUpdatedAt(Instant.now());
        InterviewRevision revision = new InterviewRevision();
        revision.setProjectId(project.getId()); revision.setRevision(project.getInterviewRevision());
        revision.setEventType(event); revision.setDocumentJson(project.getInterviewDocument());
        revisions.save(revision);
        log.info("REVISION", "project=" + project.getId() + " revision=" + project.getInterviewRevision() + " event=" + event);
    }
    private void validateText(String value, int min, int max, String name) {
        if (value == null || value.trim().length() < min || value.trim().length() > max) {
            throw new IllegalArgumentException(name + " must contain " + min + " to " + max + " characters.");
        }
    }
}
