package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.model.enums.ProjectStatus;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.enums.RequirementSource;
import com.example.masterproject.model.enums.StudyCondition;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ProjectCategoryRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import com.example.masterproject.web.dto.CreateProjectRequest;
import com.example.masterproject.web.dto.ProjectSummaryResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final DateTimeFormatter UPDATED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final ProjectRepository projectRepository;
    private final ElicitationSessionRepository sessionRepository;
    private final RequirementSlotRepository requirementSlotRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final RequirementAssessmentService requirementAssessmentService;
    private final UserContextService userContextService;
    private final LlmCredentialService llmCredentialService;
    private final AppLog appLog;

    public ProjectService(
            ProjectRepository projectRepository,
            ElicitationSessionRepository sessionRepository,
            RequirementSlotRepository requirementSlotRepository,
            ProjectCategoryRepository projectCategoryRepository,
            RequirementAssessmentService requirementAssessmentService,
            UserContextService userContextService,
            LlmCredentialService llmCredentialService,
            AppLog appLog) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.requirementSlotRepository = requirementSlotRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.requirementAssessmentService = requirementAssessmentService;
        this.userContextService = userContextService;
        this.llmCredentialService = llmCredentialService;
        this.appLog = appLog;
    }

    @Transactional
    public Project createProject(CreateProjectRequest request) {
        User owner = userContextService.getCurrentUser();
        LlmProvider provider = request.getLlmProvider();
        if (!llmCredentialService.hasProvider(provider)) {
            throw new IllegalStateException("Configure and verify an API key for " + provider + " first.");
        }

        Set<RequirementCategory> enabledCore = new LinkedHashSet<>();
        TaxonomyCatalog.mandatoryCore().forEach(definition -> enabledCore.add(definition.category()));
        if (request.getOptionalCategories() != null) {
            request.getOptionalCategories().stream()
                    .filter(category -> !TaxonomyCatalog.require(category).mandatory())
                    .forEach(enabledCore::add);
        }

        List<RequirementCategory> ordered = new ArrayList<>();
        ordered.addAll(enabledCore);
        TaxonomyCatalog.closingMandatory().forEach(definition -> ordered.add(definition.category()));

        Project project = new Project();
        project.setOwner(owner);
        project.setTitle(provisionalTitle(request.getInitialIdea()));
        project.setInitialIdea(request.getInitialIdea());
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setLlmProvider(provider);
        project.setSimplifyModeEnabled(request.isSimplifyModeEnabled());
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        Project savedProject = projectRepository.save(project);

        ElicitationSession session = new ElicitationSession();
        session.setProject(savedProject);
        session.setConditionTag(StudyCondition.GUIDED);
        sessionRepository.save(session);

        List<RequirementSlot> slots = new ArrayList<>();
        for (RequirementCategory category : ordered) {
            TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(category);

            ProjectCategory projectCategory = new ProjectCategory();
            projectCategory.setProject(savedProject);
            projectCategory.setCategory(category);
            projectCategory.setMandatory(definition.mandatory());
            projectCategory.setMaxQuestions(definition.maxQuestions());
            projectCategory.setQuestionsAsked(0);
            projectCategoryRepository.save(projectCategory);

            RequirementSlot slot = new RequirementSlot();
            slot.setProject(savedProject);
            slot.setCategory(category);
            slot.setValue(null);
            slot.setAssessmentJson(requirementAssessmentService.emptyAssessmentJson(definition));
            slot.setCompleteness(0.0);
            slot.setSource(RequirementSource.USER);
            slot.setUpdatedAt(Instant.now());
            slots.add(requirementSlotRepository.save(slot));
        }
        try {
            requirementAssessmentService.initializeFromIdea(savedProject, slots);
        } catch (RuntimeException ex) {
            appLog.warn(
                    "PROJECT",
                    "Project #" + savedProject.getId()
                            + " was created without initial extraction because the provider was unavailable.");
        }

        appLog.info(
                "PROJECT",
                "User " + owner.getEmail() + " created project #" + savedProject.getId()
                        + " with provider " + provider + ".");
        return savedProject;
    }

    private String provisionalTitle(String initialIdea) {
        String cleaned = initialIdea.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 48) {
            return cleaned;
        }
        return cleaned.substring(0, 48).trim() + "...";
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listProjectsForCurrentUser() {
        User owner = userContextService.getCurrentUser();
        return projectRepository.findByOwnerOrderByUpdatedAtDesc(owner).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listAllProjects() {
        userContextService.requireAdmin();
        return projectRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Project getProjectForCurrentUser(Long projectId) {
        User owner = userContextService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        if (!project.getOwner().getId().equals(owner.getId())) {
            throw new ProjectAccessDeniedException(projectId);
        }
        project.getOwner().getEmail();
        return project;
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse getProjectSummaryForCurrentUser(Long projectId) {
        return toSummary(getProjectForCurrentUser(projectId));
    }

    @Transactional(readOnly = true)
    public List<RequirementSlot> getRequirementSlots(Long projectId) {
        Project project = getProjectForCurrentUser(projectId);
        return requirementSlotRepository.findByProjectOrderByCategoryAsc(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectCategory> getProjectCategories(Long projectId) {
        Project project = getProjectForCurrentUser(projectId);
        return projectCategoryRepository.findByProjectOrderByIdAsc(project);
    }

    private ProjectSummaryResponse toSummary(Project project) {
        ProjectSummaryResponse response = new ProjectSummaryResponse();
        response.setId(project.getId());
        response.setTitle(project.getTitle());
        response.setInitialIdea(project.getInitialIdea());
        response.setStatus(project.getStatus());
        response.setOwnerEmail(project.getOwner().getEmail());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        response.setUpdatedAtLabel(
                project.getUpdatedAt() == null ? null : UPDATED_FORMAT.format(project.getUpdatedAt()));
        return response;
    }
}
