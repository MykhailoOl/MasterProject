package com.example.masterproject.web.controller;

import com.example.masterproject.logging.AppLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(
        assignableTypes = {
            ProjectController.class,
            HomeController.class,
            AuthController.class,
            ProfileController.class,
            LlmSettingsController.class,
            AdminController.class,
            DevAdminLoginController.class
        })
public class HtmlFailureAdvice {

    private final AppLog appLog;

    public HtmlFailureAdvice(AppLog appLog) {
        this.appLog = appLog;
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception error, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (error instanceof AccessDeniedException || error instanceof AuthenticationException) {
            throw asRuntime(error);
        }
        String path = request == null || request.getRequestURI() == null ? "/" : request.getRequestURI();
        if (path.startsWith("/api/")) {
            return "redirect:/projects";
        }
        appLog.error("APP", "Request to " + path + " failed", error);
        redirectAttributes.addFlashAttribute("errorMessage", userFacingMessage(error));
        if (path.startsWith("/settings")) {
            return "redirect:/settings/llm";
        }
        if (path.startsWith("/admin")) {
            return "redirect:/admin";
        }
        if (path.startsWith("/projects/") && path.contains("/elicit")) {
            return "redirect:" + path.replaceFirst("/elicit.*", "/elicit");
        }
        if (path.startsWith("/projects")) {
            return "redirect:/projects";
        }
        return "redirect:/";
    }

    private String userFacingMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return "Something went wrong. Please try again.";
        }
        if (message.contains("rate limited")
                || message.contains("over quota")
                || message.contains("API key")
                || message.contains("credits")
                || message.contains("temporarily unavailable")
                || message.contains("blocked this request")) {
            return message;
        }
        return "Something went wrong. Please try again.";
    }

    private RuntimeException asRuntime(Exception error) {
        if (error instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(error);
    }
}
