package com.example.masterproject.web.controller;

import com.example.masterproject.service.UserContextService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    private final UserContextService userContextService;

    public CurrentUserAdvice(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail() {
        return userContextService.getCurrentUserEmailOrNull();
    }

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        if (userContextService.getCurrentUserEmailOrNull() == null) {
            return null;
        }
        try {
            return userContextService.getCurrentUser().getUsername();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @ModelAttribute("currentUserAdmin")
    public boolean currentUserAdmin() {
        return userContextService.isCurrentUserAdmin();
    }
}
