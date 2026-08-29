package com.example.masterproject.model.taxonomy;

import com.example.masterproject.model.enums.RequirementCategory;
import java.util.Arrays;
import java.util.List;

public final class TaxonomyCatalog {

    public record Definition(
            RequirementCategory category,
            String displayName,
            String description,
            boolean mandatory,
            int maxQuestions,
            String specHeading,
            boolean includeInSpecBody) {
    }

    private static final List<Definition> ALL = List.of(
            new Definition(
                    RequirementCategory.GOAL,
                    "Goal",
                    "What the software must achieve for its users.",
                    true,
                    2,
                    "Goals",
                    true),
            new Definition(
                    RequirementCategory.USERS_AND_ROLES,
                    "Users and roles",
                    "Who uses the system and what each role can do.",
                    true,
                    2,
                    "Users and roles",
                    true),
            new Definition(
                    RequirementCategory.CORE_FEATURES,
                    "Core features",
                    "MVP capabilities the first version must include.",
                    true,
                    3,
                    "Core features",
                    true),
            new Definition(
                    RequirementCategory.PLATFORM,
                    "Platform",
                    "Where it runs (web, mobile, desktop) and key stack constraints.",
                    true,
                    1,
                    "Platform and stack",
                    true),
            new Definition(
                    RequirementCategory.NON_GOALS,
                    "Non-goals",
                    "Explicitly out of scope for the first version.",
                    false,
                    1,
                    "Non-goals",
                    true),
            new Definition(
                    RequirementCategory.DATA_ENTITIES,
                    "Data entities",
                    "Main data the system stores and how pieces relate.",
                    false,
                    2,
                    "Data model",
                    true),
            new Definition(
                    RequirementCategory.AUTHENTICATION,
                    "Authentication",
                    "Sign-in, accounts, and access control expectations.",
                    false,
                    2,
                    "Authentication and access",
                    true),
            new Definition(
                    RequirementCategory.INTEGRATIONS,
                    "Integrations",
                    "External services, APIs, or third-party tools.",
                    false,
                    2,
                    "Integrations",
                    true),
            new Definition(
                    RequirementCategory.ERROR_HANDLING,
                    "Error handling",
                    "Important failures and how the product should respond.",
                    false,
                    1,
                    "Error handling",
                    true),
            new Definition(
                    RequirementCategory.TESTING,
                    "Testing",
                    "How success will be checked before release.",
                    false,
                    1,
                    "Testing expectations",
                    true),
            new Definition(
                    RequirementCategory.DEPLOYMENT,
                    "Deployment",
                    "Where and how the app will be hosted and released.",
                    false,
                    1,
                    "Deployment",
                    true),
            new Definition(
                    RequirementCategory.PROJECT_TITLE,
                    "Project title",
                    "Final product name chosen from AI suggestions or your own wording.",
                    true,
                    1,
                    "Project title",
                    false),
            new Definition(
                    RequirementCategory.OVERALL_IDEA,
                    "Overall idea",
                    "A short product description aligned with gathered requirements, without repeating them.",
                    true,
                    1,
                    "Summary",
                    false));

    private TaxonomyCatalog() {
    }

    public static List<Definition> all() {
        return ALL;
    }

    public static List<Definition> mandatoryCore() {
        return ALL.stream()
                .filter(Definition::mandatory)
                .filter(definition -> !isClosing(definition.category()))
                .toList();
    }

    public static List<Definition> closingMandatory() {
        return ALL.stream()
                .filter(definition -> isClosing(definition.category()))
                .toList();
    }

    public static List<Definition> mandatory() {
        return ALL.stream().filter(Definition::mandatory).toList();
    }

    public static List<Definition> optional() {
        return ALL.stream().filter(definition -> !definition.mandatory()).toList();
    }

    public static Definition require(RequirementCategory category) {
        return ALL.stream()
                .filter(definition -> definition.category() == category)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + category));
    }

    public static boolean isKnown(RequirementCategory category) {
        return Arrays.stream(RequirementCategory.values()).anyMatch(value -> value == category);
    }

    public static boolean isClosing(RequirementCategory category) {
        return category == RequirementCategory.PROJECT_TITLE
                || category == RequirementCategory.OVERALL_IDEA;
    }
}
