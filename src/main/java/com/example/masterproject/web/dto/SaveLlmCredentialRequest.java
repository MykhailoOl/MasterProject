package com.example.masterproject.web.dto;

import com.example.masterproject.model.enums.LlmProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveLlmCredentialRequest {

    @NotNull
    private LlmProvider provider;

    @NotBlank
    private String apiKey;

    public LlmProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmProvider provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
