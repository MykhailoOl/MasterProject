package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import com.example.masterproject.model.entity.*;
import com.example.masterproject.model.enums.LlmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StudyProtocolTests {
    @Test
    void studyProjectsRequireStrictSelectionAndOnePinnedProviderAndModel() {
        var environment = new MockEnvironment();
        var protocol = new StudyProtocol(environment);
        assertThatThrownBy(() -> protocol.configuredModel(LlmProvider.OPENAI)).hasMessageContaining("setup is incomplete");
        environment.setProperty("app.study.strict-model", "true");
        assertThatThrownBy(() -> protocol.configuredModel(LlmProvider.OPENAI)).hasMessageContaining("setup is incomplete");
        environment.setProperty("app.llm.openai.model", "gpt-5-mini");
        assertThat(protocol.configuredModel(LlmProvider.OPENAI)).isEqualTo("gpt-5-mini");
        environment.setProperty("app.llm.gemini.model", "pinned-gemini");
        assertThatThrownBy(() -> protocol.configuredModel(LlmProvider.GEMINI)).hasMessageContaining("setup is incomplete");
    }

    @Test
    void studySessionCannotSilentlyContinueAfterModelOrSupportSettingChanges() {
        var environment = new MockEnvironment().withProperty("app.study.strict-model", "true")
                .withProperty("app.llm.openai.model", "gpt-5-mini");
        var protocol = new StudyProtocol(environment);
        var project = new Project(); project.setLlmProvider(LlmProvider.OPENAI); project.setSimplifyModeEnabled(false);
        var session = new ElicitationSession(); session.setStudyEnrolled(true); session.setStudyModel("gpt-5-mini");
        assertThatCode(() -> protocol.validate(project, session)).doesNotThrowAnyException();
        environment.setProperty("app.llm.openai.model", "another-model");
        assertThatThrownBy(() -> protocol.validate(project, session)).hasMessageContaining("settings changed");
        environment.setProperty("app.llm.openai.model", "gpt-5-mini"); project.setSimplifyModeEnabled(true);
        assertThatThrownBy(() -> protocol.validate(project, session)).hasMessageContaining("settings changed");
    }
}
