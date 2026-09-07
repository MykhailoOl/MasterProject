package com.example.masterproject.service;

import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.CriterionStatus;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GuidedElicitationPlanner {

    static final String PRUNED_KEY = "_pruned";

    private final ObjectMapper objectMapper;

    public GuidedElicitationPlanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<ProjectCategory> nextCategory(
            List<ProjectCategory> categories,
            List<RequirementSlot> slots,
            Map<RequirementCategory, List<String>> askedFocuses) {
        Map<RequirementCategory, RequirementSlot> slotsByCategory = new EnumMap<>(RequirementCategory.class);
        slots.forEach(slot -> slotsByCategory.put(slot.getCategory(), slot));
        Map<RequirementCategory, List<String>> asked =
                askedFocuses == null ? Map.of() : askedFocuses;

        for (ProjectCategory category : categories) {
            if (TaxonomyCatalog.isClosing(category.getCategory())) {
                continue;
            }
            if (shouldContinue(
                    category,
                    slotsByCategory.get(category.getCategory()),
                    asked.getOrDefault(category.getCategory(), List.of()))) {
                return Optional.of(category);
            }
        }

        for (ProjectCategory category : categories) {
            if (!TaxonomyCatalog.isClosing(category.getCategory())) {
                continue;
            }
            if (shouldContinueClosing(category, slotsByCategory.get(category.getCategory()))) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    public Optional<TaxonomyCatalog.Criterion> nextCriterion(
            TaxonomyCatalog.Definition definition,
            String assessmentJson,
            List<String> askedFocuses) {
        List<String> asked = askedFocuses == null ? List.of() : askedFocuses;
        Map<String, CriterionStatus> statuses = statuses(definition, assessmentJson);
        Set<String> pruned = Set.copyOf(prunedCriteria(assessmentJson));

        Optional<TaxonomyCatalog.Criterion> unprobedMissing =
                firstAskable(definition, statuses, pruned, asked, CriterionStatus.MISSING, 1);
        if (unprobedMissing.isPresent()) {
            return unprobedMissing;
        }
        Optional<TaxonomyCatalog.Criterion> unprobedPartial =
                firstAskable(definition, statuses, pruned, asked, CriterionStatus.PARTIAL, 1);
        if (unprobedPartial.isPresent()) {
            return unprobedPartial;
        }
        Optional<TaxonomyCatalog.Criterion> blockingRetry =
                definition.criteria().stream()
                        .filter(criterion -> TaxonomyCatalog.isBlocking(definition.category(), criterion.id()))
                        .filter(criterion -> !pruned.contains(criterion.id()))
                        .filter(criterion -> statuses.getOrDefault(criterion.id(), CriterionStatus.MISSING)
                                != CriterionStatus.COVERED)
                        .filter(criterion -> probeCount(asked, criterion.id()) < 2)
                        .findFirst();
        if (blockingRetry.isPresent()) {
            return blockingRetry;
        }
        return Optional.empty();
    }

    private Optional<TaxonomyCatalog.Criterion> firstAskable(
            TaxonomyCatalog.Definition definition,
            Map<String, CriterionStatus> statuses,
            Set<String> pruned,
            List<String> asked,
            CriterionStatus target,
            int maxProbes) {
        return definition.criteria().stream()
                .filter(criterion -> !pruned.contains(criterion.id()))
                .filter(criterion -> statuses.getOrDefault(criterion.id(), CriterionStatus.MISSING) == target)
                .filter(criterion -> probeCount(asked, criterion.id()) < maxProbes)
                .findFirst();
    }

    public Map<String, CriterionStatus> statuses(
            TaxonomyCatalog.Definition definition,
            String assessmentJson) {
        Map<String, CriterionStatus> statuses = new LinkedHashMap<>();
        for (TaxonomyCatalog.Criterion criterion : definition.criteria()) {
            statuses.put(criterion.id(), CriterionStatus.MISSING);
        }
        if (assessmentJson == null || assessmentJson.isBlank()) {
            return statuses;
        }
        try {
            JsonNode node = objectMapper.readTree(assessmentJson);
            for (TaxonomyCatalog.Criterion criterion : definition.criteria()) {
                if (node.hasNonNull(criterion.id())) {
                    statuses.put(
                            criterion.id(),
                            CriterionStatus.valueOf(node.get(criterion.id()).asText().trim().toUpperCase()));
                }
            }
        } catch (Exception ignored) {
        }
        return statuses;
    }

    public List<String> prunedCriteria(String assessmentJson) {
        if (assessmentJson == null || assessmentJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(assessmentJson);
            JsonNode pruned = node.get(PRUNED_KEY);
            if (pruned == null || !pruned.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : pruned) {
                String id = item.asText("").trim();
                if (!id.isBlank() && !values.contains(id)) {
                    values.add(id);
                }
            }
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean shouldContinue(
            ProjectCategory category,
            RequirementSlot slot,
            List<String> askedFocuses) {
        if (category.getQuestionsAsked() >= category.getMaxQuestions()) {
            return false;
        }
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(category.getCategory());
        boolean mandatoryCore = TaxonomyCatalog.mandatoryCore().stream()
                .anyMatch(item -> item.category() == category.getCategory());
        if (mandatoryCore && category.getQuestionsAsked() == 0) {
            return true;
        }
        String assessmentJson = slot == null ? null : slot.getAssessmentJson();
        return nextCriterion(definition, assessmentJson, askedFocuses).isPresent();
    }

    private boolean shouldContinueClosing(ProjectCategory category, RequirementSlot slot) {
        if (category.getQuestionsAsked() >= category.getMaxQuestions()) {
            return false;
        }
        if (slot == null || slot.getValue() == null || slot.getValue().isBlank()) {
            return true;
        }
        return slot.getCompleteness() < 1.0;
    }

    private int probeCount(List<String> askedFocuses, String criterionId) {
        int count = 0;
        for (String asked : askedFocuses) {
            if (criterionId.equals(asked)) {
                count++;
            }
        }
        return count;
    }
}
