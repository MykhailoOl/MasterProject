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

    @ModelAttribute("currentUserAdmin")
    public boolean currentUserAdmin() {
        return userContextService.isCurrentUserAdmin();
    }
}
