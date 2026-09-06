package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.llm.LlmRuntimeSettings;
import com.example.masterproject.model.entity.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class SpecEnrichmentService {

    private final LlmCredentialService llmCredentialService;
    private final ObjectMapper objectMapper;
    private final AppLog appLog;

    public SpecEnrichmentService(
            LlmCredentialService llmCredentialService, ObjectMapper objectMapper, AppLog appLog) {
        this.llmCredentialService = llmCredentialService;
        this.objectMapper = objectMapper;
        this.appLog = appLog;
    }

    public List<String> enrichUsersAndRoles(Project project, String stakeholderText) {
        String source = stakeholderText == null ? "" : stakeholderText.trim();
        List<String> fallback = deterministicRoleEnrichment(source);
        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                You prepare coding-agent role notes from non-technical stakeholder answers.
                Keep every stakeholder fact. Add only software roles that a working product usually needs
                even when a non-programmer did not name them, especially an Admin or system-owner role
                for configuration, account management, and sensitive setup.
                Map everyday words such as customer, visitor, staff, clerk, manager, or owner into clear roles.
                Return ONLY compact JSON: {"roles":["short bullet", "..."]}.
                Each bullet must be one short coding-ready role or permission line.
                Do not remove stakeholder meaning. Do not invent product features.
                """;
        String userPrompt = """
                Product title: %s
                Initial idea: %s
                Stakeholder users/roles notes:
                %s
                """.formatted(
                project.getTitle(),
                project.getInitialIdea(),
                source.isBlank() ? "(none)" : source);

        try {
            String raw = llmCredentialService.complete(
                    project.getLlmProvider(),
                    systemPrompt,
                    userPrompt,
                    0.1,
                    settings.elicitationMaxTokens());
            List<String> parsed = parseRoles(raw);
            if (!parsed.isEmpty()) {
                return ensureAdminPresent(parsed, source);
            }
        } catch (Exception ex) {
            appLog.warn(
                    "SPEC",
                    "Using deterministic role enrichment for project #" + project.getId()
                            + " because provider enrichment failed.");
        }
        return fallback;
    }

    public List<String> enrichAuthentication(Project project, String stakeholderText, String usersText) {
        String source = stakeholderText == null ? "" : stakeholderText.trim();
        String users = usersText == null ? "" : usersText.trim();
        List<String> fallback = deterministicAuthEnrichment(source, users);
        LlmRuntimeSettings settings = LlmRuntimeSettings.forProvider(project.getLlmProvider());
        String systemPrompt = """
                You prepare coding-agent access notes from non-technical answers.
                Keep stakeholder facts. Add only necessary access defaults a working product needs,
                such as protected admin/manager actions and basic sign-in expectations when accounts exist.
                Return ONLY compact JSON: {"roles":["short bullet", "..."]}.
                Do not invent unrelated security products or cloud vendors.
                """;
        String userPrompt = """
                Product title: %s
                Users and roles notes:
                %s
                Authentication notes:
                %s
                """.formatted(
                project.getTitle(),
                users.isBlank() ? "(none)" : users,
                source.isBlank() ? "(none)" : source);
        try {
            String raw = llmCredentialService.complete(
                    project.getLlmProvider(),
                    systemPrompt,
                    userPrompt,
                    0.1,
                    settings.elicitationMaxTokens());
            List<String> parsed = parseRoles(raw);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (Exception ex) {
            appLog.warn(
                    "SPEC",
                    "Using deterministic authentication enrichment for project #" + project.getId()
                            + " because provider enrichment failed.");
        }
        return fallback;
    }

    private List<String> deterministicRoleEnrichment(String stakeholderText) {
        String lower = stakeholderText.toLowerCase(Locale.ROOT);
        List<String> roles = new ArrayList<>();
        if (containsAny(lower, "customer", "visitor", "shopper", "parent", "student", "guest", "buyer")) {
            roles.add("End user / customer: use the main product flows without system setup powers.");
        } else if (!stakeholderText.isBlank()) {
            roles.add("End user: use the main product flows described by the stakeholder.");
        } else {
            roles.add("End user: use the main product flows.");
        }
        if (containsAny(lower, "staff", "clerk", "employee", "operator", "teacher", "worker")) {
            roles.add("Staff operator: perform day-to-day work actions inside the business.");
        }
        if (containsAny(lower, "manager", "owner", "admin", "administrator")) {
            roles.add("Manager / owner: control sensitive business actions such as pricing, overrides, or staff accounts.");
        }
        if (!lower.contains("admin") && !lower.contains("administrator")) {
            roles.add(
                    "Admin: configure the system, manage accounts, and perform sensitive setup that ordinary users must not do.");
        }
        return roles;
    }

    private List<String> deterministicAuthEnrichment(String authText, String usersText) {
        String combined = (authText + " " + usersText).toLowerCase(Locale.ROOT);
        List<String> notes = new ArrayList<>();
        if (containsAny(combined, "staff", "manager", "admin", "account", "sign in", "login")) {
            notes.add("Protected accounts are required for staff, managers, and admin actions.");
        } else {
            notes.add("Public visitors may use open flows; privileged actions require signed-in accounts.");
        }
        if (!combined.contains("admin")) {
            notes.add("Admin account can manage users and system configuration.");
        }
        notes.add("Ordinary users must not access admin or manager-only actions.");
        return notes;
    }

    private List<String> ensureAdminPresent(List<String> roles, String stakeholderText) {
        String joined = String.join(" ", roles).toLowerCase(Locale.ROOT)
                + " "
                + stakeholderText.toLowerCase(Locale.ROOT);
        if (!joined.contains("admin") && !joined.contains("administrator")) {
            List<String> copy = new ArrayList<>(roles);
            copy.add(
                    "Admin: configure the system, manage accounts, and perform sensitive setup that ordinary users must not do.");
            return copy;
        }
        return roles;
    }

    private List<String> parseRoles(String raw) throws Exception {
        JsonNode node = objectMapper.readTree(extractJson(raw));
        JsonNode roles = node.get("roles");
        if (roles == null || !roles.isArray()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (JsonNode role : roles) {
            String text = role.asText("").replaceAll("\\s+", " ").trim();
            if (!text.isBlank()) {
                parsed.add(text);
            }
        }
        return parsed;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
