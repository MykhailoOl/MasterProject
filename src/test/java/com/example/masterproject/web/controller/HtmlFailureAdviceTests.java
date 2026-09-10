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
    void nextQuestionFailuresStayOnTheElicitPage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects/12/elicit/next");
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
                eq("Request to /projects/12/elicit/next failed"),
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

    @Test
    void unexpectedCreateFailureKeepsTheSubmittedForm() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects"); when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("initialIdea")).thenReturn("My carefully typed project idea");
        when(request.getParameter("llmProvider")).thenReturn("OPENAI");
        when(request.getParameter("studyCondition")).thenReturn("GUIDED");
        var redirect = new RedirectAttributesModelMap();
        assertThat(advice.handleUnexpected(new UnexpectedRollbackException("database failed"), request, redirect))
                .isEqualTo("redirect:/projects/new");
        var draft = (com.example.masterproject.web.dto.CreateProjectRequest) redirect.getFlashAttributes().get("createProjectRequest");
        assertThat(draft.getInitialIdea()).isEqualTo("My carefully typed project idea");
        assertThat(draft.getStudyCondition()).isEqualTo(com.example.masterproject.model.enums.StudyCondition.GUIDED);
    }

    @Test
    void unexpectedAnswerFailurePreservesDraftAndItsQuestionIdentity() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects/12/elicit/43");
        when(request.getParameter("answerText")).thenReturn("My unsaved answer");
        var redirect = new RedirectAttributesModelMap();
        assertThat(advice.handleUnexpected(new UnexpectedRollbackException("database failed"), request, redirect))
                .isEqualTo("redirect:/projects/12/elicit");
        assertThat(redirect.getFlashAttributes().get("draftQuestionId")).isEqualTo("43");
        var draft = (com.example.masterproject.web.dto.AnswerQuestionRequest) redirect.getFlashAttributes().get("answerQuestionRequest");
        assertThat(draft.getAnswerText()).isEqualTo("My unsaved answer");
    }

    @Test
    void unexpectedReviewFailurePreservesCorrectionAndTitleDrafts() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/projects/12/review/correct");
        when(request.getParameter("correction")).thenReturn("Use my corrected rule");
        when(request.getParameter("title")).thenReturn("My edited title");
        var redirect = new RedirectAttributesModelMap();
        assertThat(advice.handleUnexpected(new UnexpectedRollbackException("database failed"), request, redirect))
                .isEqualTo("redirect:/projects/12/review");
        assertThat(redirect.getFlashAttributes().get("correctionDraft")).isEqualTo("Use my corrected rule");
        assertThat(redirect.getFlashAttributes().get("titleDraft")).isEqualTo("My edited title");
    }
}
