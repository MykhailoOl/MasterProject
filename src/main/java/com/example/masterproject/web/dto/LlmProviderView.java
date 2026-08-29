package com.example.masterproject.web.dto;

import com.example.masterproject.model.enums.LlmProvider;
import java.util.List;

public class LlmProviderView {

    private LlmProvider provider;
    private String displayName;
    private boolean configured;
    private String statusLabel;
    private String lastVerifiedLabel;
    private List<String> parameters;
    private String healthCheckHint;

    public LlmProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmProvider provider) {
        this.provider = provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getLastVerifiedLabel() {
        return lastVerifiedLabel;
    }

    public void setLastVerifiedLabel(String lastVerifiedLabel) {
        this.lastVerifiedLabel = lastVerifiedLabel;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String getHealthCheckHint() {
        return healthCheckHint;
    }

    public void setHealthCheckHint(String healthCheckHint) {
        this.healthCheckHint = healthCheckHint;
    }
}
