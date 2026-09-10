package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.example.masterproject.llm.*;
import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class LlmCallAuditTests {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void persistsEffectiveSettingsAndFailureWithoutPromptsOrCredentials(boolean failure) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var mapper = new ObjectMapper();
        var log = mock(AppLog.class);
        var client = new OpenAiLlmClient(builder, mapper, log);
        var credentials = mock(UserLlmCredentialRepository.class);
        var userContext = mock(UserContextService.class);
        var encryption = mock(SecretEncryptionService.class);
        var audits = mock(LlmCallAuditRepository.class);
        var user = new User(); user.setId(1L);
        var key = new UserLlmCredential(); key.setApiKeyEnc("encrypted-secret");
        when(userContext.getCurrentUser()).thenReturn(user);
        when(credentials.findByUserAndProvider(user, LlmProvider.OPENAI)).thenReturn(Optional.of(key));
        when(encryption.decrypt("encrypted-secret")).thenReturn("private-api-key");
        var service = new LlmCredentialService(credentials, userContext, encryption,
                new LlmClientRegistry(List.of(client)), new LlmRequestExecutor(log), log, audits, mapper);
        var request = server.expect(requestTo("https://api.openai.com/v1/chat/completions"));
        if (failure) request.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"private-api-key and private answer\"}}"));
        else request.andRespond(withSuccess("{\"model\":\"gpt-5-mini-2025-08-07\",\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Useful question?\"}}]}", MediaType.APPLICATION_JSON));
        var project = new Project(); project.setId(9L); project.setLlmProvider(LlmProvider.OPENAI);
        try (var call = org.slf4j.MDC.putCloseable("call", "question-call-id")) {
            if (failure) assertThatThrownBy(() -> service.completeForProject(project, "INTERVIEW_BASELINE", "test-prompt", "private instructions", "private answer", 0.2, 1200))
                    .hasMessageContaining("over quota");
            else assertThat(service.completeForProject(project, "INTERVIEW_BASELINE", "test-prompt", "private instructions", "private answer", 0.2, 1200))
                    .isEqualTo("Useful question?");
        }
        var captor = ArgumentCaptor.forClass(LlmCallAudit.class); verify(audits).save(captor.capture());
        var audit = captor.getValue();
        assertThat(audit.getId()).isEqualTo("question-call-id");
        assertThat(audit.getOutcome()).isEqualTo(failure ? "FAILURE" : "RESPONSE");
        assertThat(audit.getRequestedTemperature()).isEqualTo(0.2);
        assertThat(audit.getMetadataJson()).contains("\"sentTemperature\":null", "provider_default_no_override")
                .doesNotContain("private-api-key", "private answer", "private instructions", "Useful question?");
        if (failure) assertThat(audit.getMetadataJson()).contains("QUOTA_OR_RATE_LIMIT", "insufficient_quota", "429");
        else assertThat(audit.getMetadataJson()).contains("gpt-5-mini-2025-08-07");
        server.verify();
    }
}
