package com.example.masterproject.web.controller;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.service.ElicitationService;
import com.example.masterproject.service.LlmCredentialService;
import com.example.masterproject.service.ProjectAccessDeniedException;
import com.example.masterproject.service.ProjectNotFoundException;
import com.example.masterproject.service.ProjectService;
import com.example.masterproject.service.SpecExportService;
import com.example.masterproject.web.dto.AnswerQuestionRequest;
import com.example.masterproject.web.dto.CreateProjectRequest;
import com.example.masterproject.web.dto.LlmProviderView;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final LlmCredentialService llmCredentialService;
    private final ElicitationService elicitationService;
    private final SpecExportService specExportService;
    private final AppLog appLog;

    public ProjectController(
            ProjectService projectService,
            LlmCredentialService llmCredentialService,
            ElicitationService elicitationService,
            SpecExportService specExportService,
            AppLog appLog) {
        this.projectService = projectService;
        this.llmCredentialService = llmCredentialService;
        this.elicitationService = elicitationService;
        this.specExportService = specExportService;
        this.appLog = appLog;
    }

    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects", projectService.listProjectsForCurrentUser());
        return "projects/list";
    }

    @GetMapping("/new")
    public String newProjectForm(Model model) {
        if (!model.containsAttribute("createProjectRequest")) {
            model.addAttribute("createProjectRequest", new CreateProjectRequest());
        }
        populateNewProjectModel(model);
        return "projects/new";
    }

    @PostMapping
    public String createProject(
            @Valid @ModelAttribute("createProjectRequest") CreateProjectRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (request.getLlmProvider() != null && !llmCredentialService.hasProvider(request.getLlmProvider())) {
            bindingResult.rejectValue(
                    "llmProvider",
                    "project.llmProvider.missing",
                    "Save and verify an API key for this provider first.");
        }
        if (bindingResult.hasErrors()) {
            populateNewProjectModel(model);
            return "projects/new";
        }
        try {
            Long projectId = projectService.createProject(request).getId();
            redirectAttributes.addFlashAttribute("message", "Project created. Start answering questions.");
            return "redirect:/projects/" + projectId + "/elicit";
        } catch (IllegalStateException ex) {
            appLog.error("PROJECT", "Project creation failed", ex);
            bindingResult.reject("project.create.failed", ex.getMessage());
            populateNewProjectModel(model);
            return "projects/new";
        }
    }

    @GetMapping("/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        model.addAttribute("project", projectService.getProjectForCurrentUser(id));
        model.addAttribute("requirementSlots", projectService.getRequirementSlots(id));
        model.addAttribute("projectCategories", projectService.getProjectCategories(id));
        model.addAttribute("latestSpec", specExportService.latestSpec(id));
        return "projects/detail";
    }

    @GetMapping("/{id}/elicit")
    public String elicit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ElicitationService.ElicitationView view = elicitationService.getOrAdvance(id);
            model.addAttribute("view", view);
            AnswerQuestionRequest answerRequest = new AnswerQuestionRequest();
            if (view.suggestedAnswer() != null) {
                answerRequest.setAnswerText(view.suggestedAnswer());
            }
            model.addAttribute("answerQuestionRequest", answerRequest);
            return "projects/elicit";
        } catch (IllegalStateException ex) {
            appLog.error("ELICITATION", "Could not start or continue elicitation for project #" + id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/projects/" + id;
        }
    }

    @PostMapping("/{id}/elicit/{questionId}")
    public String answer(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @Valid @ModelAttribute("answerQuestionRequest") AnswerQuestionRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "selectedChoice", required = false) String selectedChoice,
            Model model,
            RedirectAttributes redirectAttributes) {
        String resolvedAnswer = resolveAnswer(selectedChoice, request.getAnswerText());
        request.setAnswerText(resolvedAnswer);
        if (resolvedAnswer == null || resolvedAnswer.isBlank()) {
            bindingResult.rejectValue("answerText", "answer.required", "Answer is required");
        }
        if (bindingResult.hasErrors()) {
            ElicitationService.ElicitationView view = elicitationService.getOrAdvance(id);
            model.addAttribute("view", view);
            return "projects/elicit";
        }
        try {
            elicitationService.submitAnswer(id, questionId, resolvedAnswer);
            return "redirect:/projects/" + id + "/elicit";
        } catch (IllegalStateException ex) {
            appLog.error("ELICITATION", "Could not process an answer for project #" + id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/projects/" + id + "/elicit";
        }
    }

    @PostMapping("/{id}/export/spec")
    public String exportSpec(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        specExportService.generateSpecMarkdown(id);
        redirectAttributes.addFlashAttribute("message", "SPEC.md generated.");
        return "redirect:/projects/" + id;
    }

    @GetMapping("/{id}/export/spec/download")
    public ResponseEntity<String> downloadSpec(@PathVariable Long id) {
        ExportArtifact artifact = specExportService.latestSpec(id);
        if (artifact == null) {
            artifact = specExportService.generateSpecMarkdown(id);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SPEC.md\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(artifact.getContent());
    }

    @ExceptionHandler({ProjectNotFoundException.class, ProjectAccessDeniedException.class})
    public String handleProjectErrors() {
        return "redirect:/projects";
    }

    private void populateNewProjectModel(Model model) {
        var providers = llmCredentialService.listForCurrentUser();
        model.addAttribute("providers", providers);
        model.addAttribute(
                "hasConfiguredProvider",
                providers.stream().anyMatch(LlmProviderView::isConfigured));
        model.addAttribute("mandatoryCategories", TaxonomyCatalog.mandatoryCore());
        model.addAttribute("optionalCategories", TaxonomyCatalog.optional());
    }

    private String resolveAnswer(String selectedChoice, String answerText) {
        if (selectedChoice != null && !selectedChoice.isBlank() && !"__custom__".equals(selectedChoice)) {
            return selectedChoice.trim();
        }
        return answerText == null ? null : answerText.trim();
    }
}
