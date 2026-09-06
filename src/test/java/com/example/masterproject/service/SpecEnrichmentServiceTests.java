package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.enums.LlmProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpecEnrichmentServiceTests {

    @Test
    void deterministicRoleEnrichmentAddsAdminWhenStakeholderOnlyNamesUserAndManager() {
        LlmCredentialService llmCredentialService = mock(LlmCredentialService.class);
        when(llmCredentialService.complete(any(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new IllegalStateException("offline"));

        SpecEnrichmentService service =
                new SpecEnrichmentService(llmCredentialService, new ObjectMapper(), mock(AppLog.class));

        Project project = new Project();
        project.setTitle("Toy Store");
        project.setInitialIdea("A website for a toy store.");
        project.setLlmProvider(LlmProvider.GROK);

        List<String> roles = service.enrichUsersAndRoles(project, "manager and user");

        assertThat(roles).anyMatch(role -> role.toLowerCase().contains("admin"));
        assertThat(String.join(" ", roles).toLowerCase()).contains("manager");
    }
}
