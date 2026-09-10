package com.example.masterproject.web.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.service.*;
import com.example.masterproject.web.dto.CreateProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ProjectRecoveryTests {
    @Test
    void creationFailureReturnsTheFormWithTheOriginalIdea() {
        ProjectService projects = mock(ProjectService.class);
        LlmCredentialService credentials = mock(LlmCredentialService.class);
        when(credentials.hasProvider(LlmProvider.OPENAI)).thenReturn(true);
        when(projects.createProject(any())).thenThrow(new IllegalStateException("Provider configuration changed. Please reconnect."));
        var controller = new ProjectController(projects, credentials, mock(ElicitationService.class), mock(SpecExportService.class));
        var request = new CreateProjectRequest(); request.setInitialIdea("My carefully typed idea"); request.setLlmProvider(LlmProvider.OPENAI);
        var binding = new BeanPropertyBindingResult(request, "createProjectRequest");
        var model = new ExtendedModelMap(); model.addAttribute("createProjectRequest", request);
        assertThat(controller.createProject(request, binding, model, new RedirectAttributesModelMap())).isEqualTo("projects/new");
        assertThat(binding.getGlobalError().getDefaultMessage()).contains("reconnect");
        assertThat(model.getAttribute("createProjectRequest")).isSameAs(request);
        assertThat(request.getInitialIdea()).isEqualTo("My carefully typed idea");
    }
}
