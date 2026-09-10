package com.example.masterproject.service;

import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class InterviewPlanner {
    public static final int MAX_QUESTIONS = com.example.masterproject.model.interview.InterviewProtocol.QUESTION_BUDGET;
    private static final Set<RequirementCategory> CORE = EnumSet.of(RequirementCategory.GOAL,
            RequirementCategory.USERS_AND_ROLES, RequirementCategory.CORE_FEATURES,
            RequirementCategory.NON_GOALS, RequirementCategory.PLATFORM);

    public Optional<Focus> next(InterviewDocument document, List<Focus> asked) {
        if (asked.size() >= MAX_QUESTIONS) return Optional.empty();
        List<Focus> candidates = gaps(document);
        Set<String> finished = document.entries().stream()
                .filter(e -> e.resolution() == Resolution.DEFERRED || e.resolution() == Resolution.NOT_APPLICABLE)
                .map(e -> key(e.category(), e.capabilityId(), e.criterion()))
                .collect(java.util.stream.Collectors.toSet());
        return candidates.stream()
                .filter(f -> f.priority() == 0 || !finished.contains(f.key()))
                .filter(f -> f.priority() == 0 || !deferredScope(document, f))
                .filter(f -> attempts(asked, f.key()) < (f.priority() <= 15 ? 2 : 1))
                .filter(f -> f.priority() == 0 || asked.stream().filter(a -> a.category() == f.category()).count()
                        < (f.category() == RequirementCategory.CORE_FEATURES ? 10 : 4))
                .min(Comparator.comparingInt(Focus::priority));
    }

    public List<Focus> gaps(InterviewDocument document) {
        Map<String, Focus> result = new LinkedHashMap<>();
        for (Entry e : document.entries()) {
            if (!document.inScope(e)) continue;
            if (e.resolution() == Resolution.CONFLICT || e.resolution() == Resolution.OPEN
                    || e.resolution() == Resolution.DEFERRED) {
                result.merge(key(e.category(), e.capabilityId(), e.criterion()),
                        new Focus(e.category(), e.capabilityId(), e.criterion(), e.text(), help(),
                                e.resolution() == Resolution.CONFLICT ? 0 : 15),
                        (first, second) -> first.priority() <= second.priority() ? first : second);
            }
        }
        for (TaxonomyCatalog.Definition category : TaxonomyCatalog.all()) {
            if (!category.includeInSpecBody()) continue;
            if (notApplicable(document, category.category(), "")) continue;
            boolean relevant = CORE.contains(category.category()) || document.entries().stream()
                    .anyMatch(e -> e.category() == category.category() && e.resolution() == Resolution.CAPTURED);
            if (!relevant) {
                addGap(result, document, category.category(), "", "_applicable",
                        applicability(category.category()), 60);
                continue;
            }
            for (TaxonomyCatalog.Criterion criterion : category.criteria()) {
                if (category.category() == RequirementCategory.CORE_FEATURES
                        && !criterion.id().equals("capabilities") && !document.capabilities().isEmpty()) {
                    for (Capability capability : document.capabilities()) {
                        if (notApplicable(document, category.category(), capability.id())) continue;
                        addGap(result, document, category.category(), capability.id(), criterion.id(),
                                capabilityQuestion(capability.name(), criterion.id()), 25);
                    }
                } else {
                    int priority = TaxonomyCatalog.isBlocking(category.category(), criterion.id()) ? 30 : 45;
                    if (category.category() == RequirementCategory.CORE_FEATURES
                            && criterion.id().equals("capabilities")) priority = 20;
                    if (category.category() == RequirementCategory.GOAL && criterion.id().equals("problem")) priority = 10;
                    addGap(result, document, category.category(), "", criterion.id(), criterion.fallbackQuestion(), priority);
                }
            }
        }
        return List.copyOf(result.values());
    }

    public double coverage(InterviewDocument document, RequirementCategory category) {
        if (document.entries().stream().anyMatch(e -> e.category() == category && e.resolution() == Resolution.CONFLICT)) return 0;
        if (notApplicable(document, category, "")) return 1.0;
        List<String> targets = new ArrayList<>();
        for (TaxonomyCatalog.Criterion c : TaxonomyCatalog.require(category).criteria()) {
            if (category == RequirementCategory.CORE_FEATURES && !c.id().equals("capabilities")
                    && !document.capabilities().isEmpty()) {
                document.capabilities().stream().filter(cap -> !notApplicable(document, category, cap.id()))
                        .forEach(cap -> targets.add(key(category, cap.id(), c.id())));
            } else targets.add(key(category, "", c.id()));
        }
        if (targets.isEmpty()) return 0;
        long covered = targets.stream().filter(target -> isCovered(document, target)).count();
        return (double) covered / targets.size();
    }

    private void addGap(Map<String, Focus> result, InterviewDocument doc, RequirementCategory category,
                        String capability, String criterion, String question, int priority) {
        String key = key(category, capability, criterion);
        if (criterion.equals("capabilities") && !doc.capabilities().isEmpty()) return;
        if (!isCovered(doc, key)) result.putIfAbsent(key,
                new Focus(category, capability, criterion, question, help(), priority));
    }

    private boolean isCovered(InterviewDocument doc, String target) {
        List<Entry> matching = doc.entries().stream()
                .filter(e -> e.resolution() != Resolution.REMOVED)
                .filter(e -> key(e.category(), e.capabilityId(), e.criterion()).equals(target)).toList();
        if (!matching.isEmpty()) return matching.stream().allMatch(Entry::resolved);
        return target.equals(key(RequirementCategory.CORE_FEATURES, "", "capabilities"))
                && !doc.capabilities().isEmpty();
    }

    private boolean deferredScope(InterviewDocument doc, Focus focus) {
        return doc.entries().stream().anyMatch(e -> e.category() == focus.category()
                && e.criterion().equals("_applicable") && e.resolution() == Resolution.DEFERRED
                && (e.capabilityId().isBlank() || e.capabilityId().equals(focus.capabilityId())));
    }

    private boolean notApplicable(InterviewDocument doc, RequirementCategory category, String capability) {
        return doc.entries().stream().anyMatch(e -> e.category() == category
                && Objects.equals(e.capabilityId(), capability) && e.criterion().equals("_applicable")
                && e.resolution() == Resolution.NOT_APPLICABLE);
    }
    private long attempts(List<Focus> asked, String key) { return asked.stream().filter(a -> a.key().equals(key)).count(); }
    private String key(RequirementCategory category, String capability, String criterion) {
        return category + ":" + (capability == null ? "" : capability) + ":" + criterion;
    }
    public static String help() {
        return "A short answer is enough. Describe what you would like someone to do or see. "
                + "You do not need experience with software. You can say you are not sure yet.";
    }
    private String capabilityQuestion(String name, String criterion) {
        return switch (criterion) {
            case "workflow" -> "For “" + name + "”, what should happen from the moment someone starts until they finish?";
            case "inputs_outputs" -> "For “" + name + "”, what information should the person give?";
            case "acceptance" -> "For “" + name + "”, what would show the person that it worked?";
            default -> "What else should be clear about “" + name + "”?";
        };
    }
    private String applicability(RequirementCategory category) {
        return switch (category) {
            case DATA_ENTITIES -> "Does anything need to be remembered when someone comes back later?";
            case AUTHENTICATION -> "Does any information or action need to be private to certain people?";
            case INTEGRATIONS -> "Does your idea need to exchange information with another service you already know about?";
            case ERROR_HANDLING -> "Is there a mistake or failure you are especially worried about?";
            case TESTING -> "Besides checking the main tasks, is there anything you want to check before using it?";
            case DEPLOYMENT -> "Do you already have any needs about when or where people can start using it?";
            default -> TaxonomyCatalog.require(category).description();
        };
    }
}
