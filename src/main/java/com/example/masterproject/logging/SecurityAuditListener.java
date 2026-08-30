package com.example.masterproject.logging;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditListener {

    private final AppLog appLog;

    public SecurityAuditListener(AppLog appLog) {
        this.appLog = appLog;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        appLog.info("APP", "Application started and is ready.");
    }

    @EventListener
    public void onClose(ContextClosedEvent event) {
        appLog.info("APP", "Application stopped.");
    }

    @EventListener
    public void onLogin(AuthenticationSuccessEvent event) {
        appLog.info("AUTH", "User " + event.getAuthentication().getName() + " logged in.");
    }

    @EventListener
    public void onLoginFailed(AbstractAuthenticationFailureEvent event) {
        String name = event.getAuthentication() == null ? "unknown" : event.getAuthentication().getName();
        appLog.warn("AUTH", "Login failed for " + name + ".");
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        String name = event.getAuthentication() == null ? "unknown" : event.getAuthentication().getName();
        appLog.info("AUTH", "User " + name + " logged out.");
    }
}
