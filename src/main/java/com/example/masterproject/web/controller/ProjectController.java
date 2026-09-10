package com.example.masterproject.web.controller;

import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.service.*;
import com.example.masterproject.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projects;
    private final LlmCredentialService credentials;
    private final ElicitationService elicitation;
    private final SpecExportService exports;

    public ProjectController(ProjectService projects, LlmCredentialService credentials,
                             ElicitationService elicitation, SpecExportService exports) {
        this.projects = projects; this.credentials = credentials;
        this.elicitation = elicitation; this.exports = exports;
    }

    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects", projects.listProjectsForCurrentUser());
        model.addAttribute("hasConfiguredProvider", credentials.listForCurrentUser().stream().anyMatch(LlmProviderView::isConfigured));
        return "projects/list";
    }

    @GetMapping("/new")
    public String newProjectForm(Model model) {
        if (!model.containsAttribute("createProjectRequest")) model.addAttribute("createProjectRequest", new CreateProjectRequest());
        populate(model);
        return "projects/new";
    }

    @PostMapping
    public String createProject(@Valid @ModelAttribute("createProjectRequest") CreateProjectRequest request,
                                BindingResult binding, Model model, RedirectAttributes redirect) {
        if (request.getLlmProvider() != null && !credentials.hasProvider(request.getLlmProvider())) {
            binding.rejectValue("llmProvider", "missing", "Connect an AI helper first.");
        }
        if (binding.hasErrors()) { populate(model); return "projects/new"; }
        try {
            Long id = projects.createProject(request).getId();
            return "redirect:/projects/" + id + "/elicit";
        } catch (IllegalStateException ex) {
            binding.reject("creationFailed", ex.getMessage());
            populate(model);
            return "projects/new";
        }
    }

    @GetMapping("/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        model.addAttribute("project", projects.getProjectForCurrentUser(id));
        model.addAttribute("requirementSlots", projects.getRequirementSlots(id));
        model.addAttribute("projectCategories", projects.getProjectCategories(id));
        model.addAttribute("latestSpec", exports.latestSpec(id));
        return "projects/detail";
    }

    @GetMapping("/{id}/elicit")
    public String elicit(@PathVariable Long id, Model model) {
        var view = elicitation.view(id);
        if (view.complete()) return "redirect:/projects/" + id + "/review";
        model.addAttribute("view", view);
        Object draftQuestion = model.getAttribute("draftQuestionId");
        if (draftQuestion != null && (view.currentQuestion() == null
                || !String.valueOf(view.currentQuestion().getId()).equals(String.valueOf(draftQuestion)))) {
            model.asMap().remove("answerQuestionRequest");
        }
        if (!model.containsAttribute("answerQuestionRequest")) model.addAttribute("answerQuestionRequest", new AnswerQuestionRequest());
        return "projects/elicit";
    }

    @PostMapping("/{id}/elicit/next")
    public String advance(@PathVariable Long id, RedirectAttributes redirect) {
        try { elicitation.getOrAdvance(id); }
        catch (IllegalStateException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/projects/" + id + "/elicit";
    }

    @PostMapping("/{id}/elicit/{questionId:\\d+}")
    public String answer(@PathVariable Long id, @PathVariable Long questionId,
                         @ModelAttribute AnswerQuestionRequest request,
                         @RequestParam(defaultValue = "ANSWER") String answerAction, RedirectAttributes redirect) {
        String answer = request.getAnswerText();
        if ("UNSURE".equals(answerAction)) {
            answer = answer == null || answer.isBlank() ? "I do not know yet; leave this decision open."
                    : answer.trim() + "\n\nI am unsure about the remaining details; leave those decisions open.";
        }
        try { elicitation.submitAnswer(id, questionId, answer); }
        catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            redirect.addFlashAttribute("answerQuestionRequest", request);
            redirect.addFlashAttribute("draftQuestionId", questionId);
        }
        return "redirect:/projects/" + id + "/elicit";
    }

    @PostMapping("/{id}/elicit/finish")
    public String finish(@PathVariable Long id, @RequestParam long revision, RedirectAttributes redirect) {
        try { elicitation.finish(id, revision); }
        catch (IllegalStateException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/projects/" + id + "/review";
    }

    @PostMapping("/{id}/export/spec")
    public String exportSpec(@PathVariable Long id, RedirectAttributes redirect) {
        try { exports.generateSpecMarkdown(id); }
        catch (IllegalStateException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/projects/" + id + "/review";
    }

    @GetMapping("/{id}/export/spec/download")
    public ResponseEntity<String> downloadSpec(@PathVariable Long id) {
        ExportArtifact artifact = exports.latestSpec(id);
        if (artifact == null) return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.TEXT_PLAIN)
                .body("Review and confirm the current plan before downloading its SPEC file.");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SPEC.md\"")
                .contentType(new MediaType("text", "markdown", java.nio.charset.StandardCharsets.UTF_8)).body(artifact.getContent());
    }

    @ExceptionHandler({ProjectNotFoundException.class, ProjectAccessDeniedException.class})
    public String handleProjectErrors() { return "redirect:/projects"; }

    private void populate(Model model) {
        var providers = credentials.listForCurrentUser();
        model.addAttribute("providers", providers);
        model.addAttribute("hasConfiguredProvider", providers.stream().anyMatch(LlmProviderView::isConfigured));
    }
}
