package com.example.masterproject.service;

import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.CriterionStatus;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import java.util.Comparator;
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

    private final ObjectMapper objectMapper;

    public GuidedElicitationPlanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<ProjectCategory> nextCategory(
            List<ProjectCategory> categories,
            List<RequirementSlot> slots) {
        Map<RequirementCategory, RequirementSlot> slotsByCategory = new EnumMap<>(RequirementCategory.class);
        slots.forEach(slot -> slotsByCategory.put(slot.getCategory(), slot));

        List<ProjectCategory> bodyCandidates = categories.stream()
                .filter(category -> !TaxonomyCatalog.isClosing(category.getCategory()))
                .filter(category -> category.getQuestionsAsked() < category.getMaxQuestions())
                .filter(category -> completeness(category, slotsByCategory) < 0.95)
                .toList();

        List<ProjectCategory> foundationalCandidates = bodyCandidates.stream()
                .filter(ProjectCategory::isMandatory)
                .filter(category -> completeness(category, slotsByCategory) < 0.75)
                .toList();

        List<ProjectCategory> pool =
                foundationalCandidates.isEmpty() ? bodyCandidates : foundationalCandidates;
        if (!pool.isEmpty()) {
            return pool.stream().min(categoryComparator(categories, slotsByCategory));
        }

        return categories.stream()
                .filter(category -> TaxonomyCatalog.isClosing(category.getCategory()))
                .filter(category -> category.getQuestionsAsked() < category.getMaxQuestions())
                .filter(category -> completeness(category, slotsByCategory) < 0.95)
                .findFirst();
    }

    public TaxonomyCatalog.Criterion nextCriterion(
            TaxonomyCatalog.Definition definition,
            String assessmentJson,
            Set<String> previouslyAsked) {
        Map<String, CriterionStatus> statuses = statuses(definition, assessmentJson);
        for (CriterionStatus target : List.of(CriterionStatus.MISSING, CriterionStatus.PARTIAL)) {
            Optional<TaxonomyCatalog.Criterion> unasked = definition.criteria().stream()
                    .filter(criterion -> statuses.get(criterion.id()) == target)
                    .filter(criterion -> !previouslyAsked.contains(criterion.id()))
                    .findFirst();
            if (unasked.isPresent()) {
                return unasked.get();
            }
        }
        for (CriterionStatus target : List.of(CriterionStatus.MISSING, CriterionStatus.PARTIAL)) {
            Optional<TaxonomyCatalog.Criterion> remaining = definition.criteria().stream()
                    .filter(criterion -> statuses.get(criterion.id()) == target)
                    .findFirst();
            if (remaining.isPresent()) {
                return remaining.get();
            }
        }
        return definition.criteria().stream()
                .filter(criterion -> !previouslyAsked.contains(criterion.id()))
                .findFirst()
                .orElseGet(() -> definition.criteria().getFirst());
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

    private Comparator<ProjectCategory> categoryComparator(
            List<ProjectCategory> ordered,
            Map<RequirementCategory, RequirementSlot> slotsByCategory) {
        return Comparator
                .comparingDouble((ProjectCategory category) -> completeness(category, slotsByCategory))
                .thenComparingInt(ProjectCategory::getQuestionsAsked)
                .thenComparing(category -> !category.isMandatory())
                .thenComparingInt(ordered::indexOf);
    }

    private double completeness(
            ProjectCategory category,
            Map<RequirementCategory, RequirementSlot> slotsByCategory) {
        RequirementSlot slot = slotsByCategory.get(category.getCategory());
        if (slot == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, slot.getCompleteness()));
    }
}
