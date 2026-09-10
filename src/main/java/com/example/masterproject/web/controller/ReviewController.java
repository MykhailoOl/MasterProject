package com.example.masterproject.web.controller;

import com.example.masterproject.service.*;
import com.example.masterproject.model.interview.InterviewDocument.Resolution;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{id}/review")
public class ReviewController {
    private final InterviewStore store;
    private final ElicitationService elicitation;
    private final SpecRenderer renderer;
    private final SpecExportService exports;
    private final InterviewPlanner planner;

    public ReviewController(InterviewStore store, ElicitationService elicitation, SpecRenderer renderer,
                            SpecExportService exports, InterviewPlanner planner) {
        this.store = store; this.elicitation = elicitation; this.renderer = renderer;
        this.exports = exports; this.planner = planner;
    }

    @GetMapping
    public String review(@PathVariable Long id, Model model) {
        var snapshot = store.load(id);
        if (snapshot.project().isCurrentProtocol() && snapshot.project().getStatus() == com.example.masterproject.model.enums.ProjectStatus.IN_PROGRESS) {
            return "redirect:/projects/" + id + "/elicit";
        }
        var document = snapshot.document();
        String summary = document.overview();
        model.addAttribute("project", snapshot.project());
        model.addAttribute("document", document);
        model.addAttribute("summary", summary);
        model.addAttribute("entries", document.entries().stream()
                .filter(e -> e.resolution() != Resolution.REMOVED)
                .filter(e -> e.resolution() == Resolution.NOT_APPLICABLE || document.inScope(e)).toList());
        model.addAttribute("sources", snapshot.sources());
        model.addAttribute("gaps", planner.gaps(document));
        model.addAttribute("needsAssessment", !snapshot.assessed() || !document.warnings().isEmpty());
        model.addAttribute("latestSpec", exports.latestSpec(id));
        model.addAttribute("preview", renderer.render(snapshot.project().getTitle(), summary, document,
                snapshot.project().getReviewedRevision() == snapshot.project().getInterviewRevision()));
        return "projects/review";
    }

    @PostMapping("/retry")
    public String retry(@PathVariable Long id, RedirectAttributes redirect) {
        try { elicitation.refreshAssessment(id); }
        catch (IllegalStateException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
        return back(id);
    }

    @PostMapping("/correct")
    public String correct(@PathVariable Long id, @RequestParam long revision,
                          @RequestParam String correction,
                          @RequestParam(required = false) String entryId, RedirectAttributes redirect) {
        String originalDraft = correction;
        try {
            if (entryId != null && !entryId.isBlank()) {
                var snapshot = store.load(id);
                if (snapshot.document().entries().stream().noneMatch(e -> e.id().equals(entryId))) {
                    throw new IllegalArgumentException("That statement is no longer available. Reload the page.");
                }
                correction = "Replace statement " + entryId + " with this correction: " + correction;
            }
            elicitation.correct(id, revision, correction);
            redirect.addFlashAttribute("message", "Your correction was saved. Please check the updated plan.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            redirect.addFlashAttribute("correctionDraft", originalDraft);
        }
        return back(id);
    }

    @PostMapping("/confirm")
    public String confirm(@PathVariable Long id, @RequestParam long revision, @RequestParam String title,
                          @RequestParam(defaultValue = "false") boolean accepted,
                          RedirectAttributes redirect) {
        try {
            store.confirm(id, revision, title, accepted);
            exports.generateSpecMarkdown(id);
            redirect.addFlashAttribute("message", "Your reviewed SPEC file is ready to download.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            redirect.addFlashAttribute("titleDraft", title);
        }
        return back(id);
    }

    @ExceptionHandler({ProjectAccessDeniedException.class, ProjectNotFoundException.class})
    public String denied() { return "redirect:/projects"; }
    private String back(Long id) { return "redirect:/projects/" + id + "/review"; }
}
