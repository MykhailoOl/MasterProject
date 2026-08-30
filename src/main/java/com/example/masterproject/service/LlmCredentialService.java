package com.example.masterproject.service;

import com.example.masterproject.llm.LlmClient;
import com.example.masterproject.llm.LlmClientRegistry;
import com.example.masterproject.llm.LlmHealthResult;
import com.example.masterproject.llm.LlmRuntimeSettings;
import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.entity.UserLlmCredential;
import com.example.masterproject.model.enums.LlmProvider;
import com.example.masterproject.repository.UserLlmCredentialRepository;
import com.example.masterproject.web.dto.LlmProviderView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LlmCredentialService {

    private static final DateTimeFormatter VERIFIED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final UserLlmCredentialRepository credentialRepository;
    private final UserContextService userContextService;
    private final SecretEncryptionService encryptionService;
    private final LlmClientRegistry llmClientRegistry;
    private final AppLog appLog;

    public LlmCredentialService(
            UserLlmCredentialRepository credentialRepository,
            UserContextService userContextService,
            SecretEncryptionService encryptionService,
            LlmClientRegistry llmClientRegistry,
            AppLog appLog) {
        this.credentialRepository = credentialRepository;
        this.userContextService = userContextService;
        this.encryptionService = encryptionService;
        this.llmClientRegistry = llmClientRegistry;
        this.appLog = appLog;
    }

    @Transactional(readOnly = true)
    public List<LlmProviderView> listForCurrentUser() {
        User user = userContextService.getCurrentUser();
        Map<LlmProvider, UserLlmCredential> byProvider = new LinkedHashMap<>();
        credentialRepository.findByUserOrderByProviderAsc(user)
                .forEach(credential -> byProvider.put(credential.getProvider(), credential));

        return Arrays.stream(LlmProvider.values())
                .map(provider -> toView(provider, byProvider.get(provider)))
                .toList();
    }

    @Transactional
    public LlmHealthResult saveAndVerify(LlmProvider provider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return new LlmHealthResult(false, "API key is required.");
        }
        String trimmed = apiKey.trim();
        LlmClient client = llmClientRegistry.require(provider);
        LlmHealthResult health = client.checkHealth(trimmed);
        if (!health.ok()) {
            appLog.warn(
                    "LLM",
                    "API key check failed for " + displayName(provider) + " by "
                            + userContextService.getCurrentUserEmailOrNull() + ": " + health.message());
            return health;
        }

        User user = userContextService.getCurrentUser();
        var existing = credentialRepository.findByUserAndProvider(user, provider);
        boolean updating = existing.isPresent();
        UserLlmCredential credential = existing.orElseGet(UserLlmCredential::new);
        credential.setUser(user);
        credential.setProvider(provider);
        credential.setApiKeyEnc(encryptionService.encrypt(trimmed));
        credential.setLastVerifiedAt(Instant.now());
        credential.setUpdatedAt(Instant.now());
        if (credential.getCreatedAt() == null) {
            credential.setCreatedAt(Instant.now());
        }
        credentialRepository.save(credential);
        String message = displayName(provider) + " API key "
                + (updating ? "replaced" : "saved")
                + " and verified.";
        appLog.info(
                "LLM",
                "User " + user.getEmail() + (updating ? " replaced" : " saved")
                        + " API key for " + displayName(provider) + ".");
        return new LlmHealthResult(true, message);
    }

    @Transactional(readOnly = true)
    public LlmHealthResult verifyStored(LlmProvider provider) {
        String apiKey = resolveApiKey(provider);
        return llmClientRegistry.require(provider).checkHealth(apiKey);
    }

    @Transactional(readOnly = true)
    public boolean hasProvider(LlmProvider provider) {
        User user = userContextService.getCurrentUser();
        return credentialRepository.existsByUserAndProvider(user, provider);
    }

    @Transactional(readOnly = true)
    public String resolveApiKey(LlmProvider provider) {
        User user = userContextService.getCurrentUser();
        UserLlmCredential credential = credentialRepository
                .findByUserAndProvider(user, provider)
                .orElseThrow(() -> new IllegalStateException("No API key configured for " + provider));
        return encryptionService.decrypt(credential.getApiKeyEnc());
    }

    @Transactional(readOnly = true)
    public String complete(
            LlmProvider provider, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        String apiKey = resolveApiKey(provider);
        appLog.info("LLM", "Calling " + displayName(provider) + " for " + userContextService.getCurrentUserEmailOrNull() + ".");
        return llmClientRegistry.require(provider).complete(apiKey, systemPrompt, userPrompt, temperature, maxTokens);
    }

    private LlmProviderView toView(LlmProvider provider, UserLlmCredential credential) {
        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(provider);
        LlmProviderView view = new LlmProviderView();
        view.setProvider(provider);
        view.setDisplayName(displayName(provider));
        view.setConfigured(credential != null);
        view.setStatusLabel(credential != null ? "Configured" : "Not configured");
        view.setLastVerifiedLabel(
                credential == null || credential.getLastVerifiedAt() == null
                        ? null
                        : VERIFIED_FORMAT.format(credential.getLastVerifiedAt()));
        view.setParameters(List.of(
                "model=" + settings.model(),
                "elicitation temperature=" + settings.elicitationTemperature(),
                "simplify temperature=" + settings.simplifyTemperature(),
                "elicitation max output tokens=" + settings.elicitationMaxTokens(),
                "simplify max output tokens=" + settings.simplifyMaxTokens(),
                "health check=" + settings.healthCheckDescription()));
        view.setHealthCheckHint(settings.healthCheckDescription());
        return view;
    }

    private String displayName(LlmProvider provider) {
        return switch (provider) {
            case OPENAI -> "OpenAI";
            case ANTHROPIC -> "Anthropic";
            case GEMINI -> "Google Gemini (free tier)";
            case GROK -> "xAI Grok";
        };
    }
}
