package com.example.masterproject.model.interview;

import com.example.masterproject.model.enums.RequirementCategory;
import java.util.List;

public record InterviewDocument(
        String protocolVersion,
        List<Capability> capabilities,
        List<Entry> entries,
        List<String> processedSources,
        List<String> warnings,
        String summary,
        List<SourceCheck> sourceChecks,
        List<ClaimCheck> claimChecks) {
    public static final String VERSION = "ideaspec-interview-3";

    public InterviewDocument(String protocolVersion, List<Capability> capabilities, List<Entry> entries,
                             List<String> processedSources, List<String> warnings, String summary) {
        this(protocolVersion, capabilities, entries, processedSources, warnings, summary, List.of(), List.of());
    }
    public InterviewDocument(String protocolVersion, List<Capability> capabilities, List<Entry> entries,
                             List<String> processedSources, List<String> warnings, String summary, List<SourceCheck> sourceChecks) {
        this(protocolVersion, capabilities, entries, processedSources, warnings, summary, sourceChecks, List.of());
    }

    public InterviewDocument {
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        entries = entries == null ? List.of() : List.copyOf(entries);
        processedSources = processedSources == null ? List.of() : List.copyOf(processedSources);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        summary = summary == null ? "" : summary;
        sourceChecks = sourceChecks == null ? List.of() : List.copyOf(sourceChecks);
        claimChecks = claimChecks == null ? List.of() : List.copyOf(claimChecks);
    }

    public static InterviewDocument empty() {
        return new InterviewDocument(VERSION, List.of(), List.of(), List.of(), List.of(), "");
    }
    public boolean hasCheckedSource(String id) {
        return processedSources.contains(id) && sourceChecks.stream().anyMatch(c -> c.source().equals(id) && c.verified());
    }

    public boolean inScope(Entry entry) {
        if (entry.resolution() == Resolution.CONFLICT) return true;
        return entries.stream().noneMatch(scope -> scope.resolution() == Resolution.NOT_APPLICABLE
                && scope.criterion().equals("_applicable") && scope.category() == entry.category()
                && (scope.capabilityId().isBlank() || scope.capabilityId().equals(entry.capabilityId())));
    }

    public String overview() {
        List<Entry> goals = entries.stream().filter(this::inScope)
                .filter(e -> e.resolution() == Resolution.CAPTURED && e.category() == RequirementCategory.GOAL).toList();
        List<Entry> selected = goals.isEmpty() ? entries.stream().filter(this::inScope)
                .filter(e -> e.resolution() == Resolution.CAPTURED && e.category() != RequirementCategory.NON_GOALS)
                .toList() : goals;
        if (selected.isEmpty()) return activeCapabilities().stream().map(Capability::name)
                .collect(java.util.stream.Collectors.joining("; "));
        return selected.stream().limit(3).map(Entry::text).collect(java.util.stream.Collectors.joining(" "));
    }

    public List<Capability> activeCapabilities() {
        return capabilities.stream().filter(cap -> inScope(new Entry("", RequirementCategory.CORE_FEATURES,
                cap.id(), "capabilities", cap.name(), Resolution.CAPTURED, cap.evidence()))).toList();
    }

    public record Evidence(String source, String quote) {}
    public record SourceCheck(String source, String outcome, String reason, boolean verified, String verificationReason) {
        public SourceCheck(String source, String outcome, String reason, boolean verified) { this(source, outcome, reason, verified, ""); }
    }
    public record ClaimCheck(String id, String reason) {}
    public record Capability(String id, String name, List<Evidence> evidence) {}
    public enum Resolution { CAPTURED, OPEN, DEFERRED, NOT_APPLICABLE, CONFLICT, REMOVED }
    public record Entry(
            String id, RequirementCategory category, String capabilityId, String criterion,
            String text, Resolution resolution, List<Evidence> evidence) {
        public boolean resolved() {
            return resolution == Resolution.CAPTURED || resolution == Resolution.NOT_APPLICABLE;
        }
    }
    public record Source(String id, String question, String text, String provenance) {
        public Source(String id, String question, String text) { this(id, question, text, "STAKEHOLDER"); }
    }
    public record Focus(RequirementCategory category, String capabilityId, String criterion,
                        String question, String help, int priority) {
        public String key() {
            return category.name() + ":" + (capabilityId == null ? "" : capabilityId) + ":" + criterion;
        }
    }
}
