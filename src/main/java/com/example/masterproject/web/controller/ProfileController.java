package com.example.masterproject.web.controller;

import com.example.masterproject.model.entity.User;
import com.example.masterproject.service.EmailAlreadyUsedException;
import com.example.masterproject.service.InvalidProfileUpdateException;
import com.example.masterproject.service.ProfileService;
import com.example.masterproject.web.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public ProfileController(
            ProfileService profileService,
            UserDetailsService userDetailsService,
            SecurityContextRepository securityContextRepository) {
        this.profileService = profileService;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    public String profile(Model model) {
        if (!model.containsAttribute("updateProfileRequest")) {
            User user = profileService.getCurrentProfile();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setEmail(user.getEmail());
            request.setDisplayName(user.getDisplayName());
            model.addAttribute("updateProfileRequest", request);
        }
        model.addAttribute("profileUser", profileService.getCurrentProfile());
        return "profile";
    }

    @PostMapping
    public String update(
            @Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileUser", profileService.getCurrentProfile());
            return "profile";
        }
        try {
            User updated = profileService.updateProfile(request);
            refreshAuthentication(updated.getEmail(), httpRequest, httpResponse);
            redirectAttributes.addFlashAttribute("message", "Profile updated.");
            return "redirect:/profile";
        } catch (EmailAlreadyUsedException ex) {
            bindingResult.rejectValue("email", "auth.email.taken", "Email is already registered");
            model.addAttribute("profileUser", profileService.getCurrentProfile());
            return "profile";
        } catch (InvalidProfileUpdateException ex) {
            bindingResult.reject("profile.update.failed", ex.getMessage());
            model.addAttribute("profileUser", profileService.getCurrentProfile());
            return "profile";
        }
    }

    private void refreshAuthentication(
            String email, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, details.getPassword(), details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
