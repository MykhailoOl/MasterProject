package com.example.masterproject.web.controller;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.llm.LlmHealthResult;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.service.LlmCredentialService;
import com.example.masterproject.web.dto.SaveLlmCredentialRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/llm")
public class LlmSettingsController {

    private final LlmCredentialService llmCredentialService;
    private final AppLog appLog;

    public LlmSettingsController(LlmCredentialService llmCredentialService, AppLog appLog) {
        this.llmCredentialService = llmCredentialService;
        this.appLog = appLog;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("providers", llmCredentialService.listForCurrentUser());
        if (!model.containsAttribute("saveLlmCredentialRequest")) {
            model.addAttribute("saveLlmCredentialRequest", new SaveLlmCredentialRequest());
        }
        return "settings/llm";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("saveLlmCredentialRequest") SaveLlmCredentialRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("providers", llmCredentialService.listForCurrentUser());
            return "settings/llm";
        }
        try {
            LlmHealthResult result = llmCredentialService.saveAndVerify(request.getProvider(), request.getApiKey());
            redirectAttributes.addFlashAttribute(result.ok() ? "message" : "errorMessage", result.message());
        } catch (RuntimeException ex) {
            appLog.error("LLM", "Could not save API key for " + request.getProvider(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "The API key could not be saved. Please try again.");
        }
        return "redirect:/settings/llm";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam LlmProvider provider, RedirectAttributes redirectAttributes) {
        try {
            LlmHealthResult result = llmCredentialService.verifyStored(provider);
            redirectAttributes.addFlashAttribute(result.ok() ? "message" : "errorMessage", result.message());
        } catch (Exception ex) {
            appLog.error("LLM", "Stored key check failed for " + provider, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "The saved API key could not be checked.");
        }
        return "redirect:/settings/llm";
    }

    @PostMapping(value = "/verify-live", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyLive(@RequestParam LlmProvider provider) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            LlmHealthResult result = llmCredentialService.verifyStored(provider);
            body.put("ok", result.ok());
            body.put("message", result.message());
            body.put("lastVerifiedLabel", llmCredentialService.lastVerifiedLabel(provider));
            body.put("statusLabel", result.ok() ? "Ready" : "Check failed");
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            appLog.error("LLM", "Live key check failed for " + provider, ex);
            body.put("ok", false);
            body.put("message", "The saved API key could not be checked.");
            body.put("lastVerifiedLabel", null);
            body.put("statusLabel", "Check failed");
            return ResponseEntity.ok(body);
        }
    }
}
