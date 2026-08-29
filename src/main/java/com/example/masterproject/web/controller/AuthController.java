package com.example.masterproject.web.controller;

import com.example.masterproject.service.AuthService;
import com.example.masterproject.service.EmailAlreadyUsedException;
import com.example.masterproject.service.UserContextService;
import com.example.masterproject.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final UserContextService userContextService;
    private final boolean quickAdminLogin;

    public AuthController(
            AuthService authService,
            UserContextService userContextService,
            @Value("${app.dev.quick-admin-login:false}") boolean quickAdminLogin) {
        this.authService = authService;
        this.userContextService = userContextService;
        this.quickAdminLogin = quickAdminLogin;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        if (userContextService.getCurrentUserEmailOrNull() != null) {
            return "redirect:/";
        }
        model.addAttribute("quickAdminLogin", quickAdminLogin);
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (userContextService.getCurrentUserEmailOrNull() != null) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (userContextService.getCurrentUserEmailOrNull() != null) {
            return "redirect:/";
        }
        if (request.getPassword() != null
                && request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "auth.confirmPassword.mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasFieldErrors("email") && authService.emailExists(request.getEmail())) {
            bindingResult.rejectValue("email", "auth.email.taken", "Email is already registered");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(request);
        } catch (EmailAlreadyUsedException ex) {
            bindingResult.rejectValue("email", "auth.email.taken", "Email is already registered");
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("message", "Account created. Please log in.");
        return "redirect:/login";
    }
}
