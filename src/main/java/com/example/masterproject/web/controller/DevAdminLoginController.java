package com.example.masterproject.web.controller;

import com.example.masterproject.security.AppUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DevAdminLoginController {

    private final AppUserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final boolean quickAdminLogin;
    private final String adminEmail;

    public DevAdminLoginController(
            AppUserDetailsService userDetailsService,
            SecurityContextRepository securityContextRepository,
            @Value("${app.dev.quick-admin-login:false}") boolean quickAdminLogin,
            @Value("${app.seed.admin-email}") String adminEmail) {
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.quickAdminLogin = quickAdminLogin;
        this.adminEmail = adminEmail;
    }

    @PostMapping("/dev/login-as-admin")
    public String loginAsAdmin(
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (!quickAdminLogin) {
            return "redirect:/login";
        }
        request.getSession(true);
        UserDetails admin = userDetailsService.loadUserByUsername(adminEmail);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        redirectAttributes.addFlashAttribute("message", "Signed in as admin (dev shortcut).");
        return "redirect:/";
    }
}
