package com.example.masterproject.llm;

import com.example.masterproject.logging.AppLog;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LlmUsageLogTests {
    @Test
    void recordsActualReturnedModelAndUsageWithoutResponseText() {
        AppLog log = mock(AppLog.class);
        var root = new ObjectMapper().readTree("""
                {"model":"actual-model","usage":{"prompt_tokens":100,"completion_tokens":20,"total_tokens":120},
                "choices":[{"message":{"content":"Private stakeholder notes"}}]}
                """);
        LlmUsageLog.record(log, "requested-model", root);
        verify(log).info(eq("LLM_USAGE"), contains("actual_model=actual-model input_tokens=100 output_tokens=20"));
        verify(log, never()).info(anyString(), contains("Private stakeholder notes"));
    }

    @Test
    void unavailableUsageIsMarkedUnknownInsteadOfZero() {
        AppLog log = mock(AppLog.class);
        LlmUsageLog.record(log, "model", new ObjectMapper().readTree("{}"));
        verify(log).info(eq("LLM_USAGE"), contains("input_tokens=-1 output_tokens=-1"));
    }
}
