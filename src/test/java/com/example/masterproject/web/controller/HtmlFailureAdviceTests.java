package com.example.masterproject.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.masterproject.logging.AppLog;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class HtmlFailureAdviceTests {

    private final AppLog appLog = mock(AppLog.class);
    private final HtmlFailureAdvice advice = new HtmlFailureAdvice(appLog);

    @Test
    void fastFinishFailuresStayOnTheElicitPage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects/12/elicit/fast-finish");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = advice.handleUnexpected(
                new UnexpectedRollbackException("Transaction silently rolled back"),
                request,
                redirectAttributes);

        assertThat(view).isEqualTo("redirect:/projects/12/elicit");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Something went wrong. Please try again.");
        verify(appLog).error(
                eq("APP"),
                eq("Request to /projects/12/elicit/fast-finish failed"),
                any(UnexpectedRollbackException.class));
    }

    @Test
    void llmQuotaMessagesAreShownToTheUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects/12/elicit");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = advice.handleUnexpected(
                new IllegalStateException("Gemini is rate limited or over quota. Please try again in a minute."),
                request,
                redirectAttributes);

        assertThat(view).isEqualTo("redirect:/projects/12/elicit");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Gemini is rate limited or over quota. Please try again in a minute.");
    }

    @Test
    void accessDeniedIsNotSwallowed() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() ->
                        advice.handleUnexpected(new AccessDeniedException("denied"), request, redirectAttributes))
                .isInstanceOf(AccessDeniedException.class);
    }
}
