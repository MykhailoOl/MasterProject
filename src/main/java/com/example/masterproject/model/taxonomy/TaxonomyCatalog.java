package com.example.masterproject.model.taxonomy;

import com.example.masterproject.model.enums.RequirementCategory;
import java.util.Arrays;
import java.util.List;

public final class TaxonomyCatalog {

    public record Criterion(
            String id,
            String description,
            String fallbackQuestion) {
    }

    public record Definition(
            RequirementCategory category,
            String displayName,
            String description,
            boolean mandatory,
            int maxQuestions,
            String specHeading,
            boolean includeInSpecBody,
            List<Criterion> criteria) {
    }

    private static final List<Definition> ALL = List.of(
            new Definition(
                    RequirementCategory.GOAL,
                    "Goal",
                    "The problem, intended outcome, success signals, and priorities.",
                    true,
                    2,
                    "Goals",
                    true,
                    List.of(
                            new Criterion(
                                    "problem",
                                    "The specific problem or current difficulty to solve.",
                                    "What specific problem or current difficulty should this product solve?"),
                            new Criterion(
                                    "outcome",
                                    "The outcome or benefit users should achieve.",
                                    "What outcome should users achieve with this product?"),
                            new Criterion(
                                    "success",
                                    "An observable or measurable signal of success.",
                                    "What observable result would show that the product is successful?"),
                            new Criterion(
                                    "priority",
                                    "The most important outcome when trade-offs are necessary.",
                                    "If trade-offs are necessary, which outcome is most important?"))),
            new Definition(
                    RequirementCategory.USERS_AND_ROLES,
                    "Users and roles",
                    "User groups, their goals, permissions, and usage context.",
                    true,
                    2,
                    "Users and roles",
                    true,
                    List.of(
                            new Criterion(
                                    "user_groups",
                                    "Distinct user or stakeholder groups.",
                                    "Which distinct groups of people or systems will use this product?"),
                            new Criterion(
                                    "user_goals",
                                    "The goal each user group needs to accomplish.",
                                    "What does each user group need to accomplish?"),
                            new Criterion(
                                    "permissions",
                                    "Responsibilities and access boundaries for each role.",
                                    "What should each role be allowed or prevented from doing?"),
                            new Criterion(
                                    "usage_context",
                                    "Relevant skill, accessibility, frequency, or environment constraints.",
                                    "In what context will the main users use the product?"))),
            new Definition(
                    RequirementCategory.CORE_FEATURES,
                    "Core features",
                    "Prioritized capabilities, workflows, inputs, outputs, and acceptance behaviour.",
                    true,
                    4,
                    "Core features",
                    true,
                    List.of(
                            new Criterion(
                                    "capabilities",
                                    "The essential capabilities required in the first usable release.",
                                    "Which capabilities are essential for the first usable release?"),
                            new Criterion(
                                    "workflow",
                                    "The main user workflow, including its trigger and sequence.",
                                    "Walk through the main task from its trigger to completion."),
                            new Criterion(
                                    "inputs_outputs",
                                    "Important inputs, outputs, and state changes.",
                                    "What information enters the main workflow, and what result should it produce?"),
                            new Criterion(
                                    "acceptance",
                                    "Observable acceptance behaviour and important exceptions.",
                                    "What behaviour must be observable for the main workflow to be considered correct?"))),
            new Definition(
                    RequirementCategory.PLATFORM,
                    "Platform",
                    "Delivery channel, supported environments, technical constraints, and quality expectations.",
                    true,
                    2,
                    "Platform and stack",
                    true,
                    List.of(
                            new Criterion(
                                    "delivery_channel",
                                    "Whether the product is web, mobile, desktop, API, embedded, or another channel.",
                                    "Through which channel should users access the product?"),
                            new Criterion(
                                    "supported_environments",
                                    "Required devices, operating systems, browsers, or runtime environments.",
                                    "Which devices, operating systems, browsers, or runtimes must be supported?"),
                            new Criterion(
                                    "technology_constraints",
                                    "Required or prohibited technologies and compatibility constraints.",
                                    "Are any technologies required, prohibited, or constrained by an existing environment?"),
                            new Criterion(
                                    "quality_constraints",
                                    "Platform-level performance, availability, accessibility, or offline expectations.",
                                    "Which platform-level quality constraint matters most for this product?"))),
            new Definition(
                    RequirementCategory.NON_GOALS,
                    "Non-goals",
                    "Explicit product boundaries, exclusions, deferred work, and assumptions.",
                    false,
                    1,
                    "Non-goals",
                    true,
                    List.of(
                            new Criterion(
                                    "excluded_capabilities",
                                    "Capabilities explicitly excluded from the first release.",
                                    "Which plausible capabilities must be explicitly excluded from the first release?"),
                            new Criterion(
                                    "system_boundary",
                                    "Where this product's responsibility starts and ends.",
                                    "Where should this product's responsibility end?"),
                            new Criterion(
                                    "deferred_work",
                                    "Ideas deliberately deferred to a later release.",
                                    "Which ideas should be deferred until after the first release?"),
                            new Criterion(
                                    "assumptions",
                                    "Important assumptions that prevent accidental scope expansion.",
                                    "Which assumption about the first release should be made explicit?"))),
            new Definition(
                    RequirementCategory.DATA_ENTITIES,
                    "Data entities",
                    "Core information, relationships, ownership, and lifecycle rules.",
                    false,
                    2,
                    "Data model",
                    true,
                    List.of(
                            new Criterion(
                                    "entities_attributes",
                                    "Core entities and the information each must retain.",
                                    "What core information must the product store?"),
                            new Criterion(
                                    "relationships",
                                    "Relationships and cardinality between core entities.",
                                    "How are the main pieces of stored information related?"),
                            new Criterion(
                                    "ownership_access",
                                    "Who owns, creates, reads, and changes the data.",
                                    "Who owns the stored information, and who may view or change it?"),
                            new Criterion(
                                    "lifecycle",
                                    "Creation, update, deletion, retention, and audit rules.",
                                    "What lifecycle or retention rules apply to the stored information?"))),
            new Definition(
                    RequirementCategory.AUTHENTICATION,
                    "Authentication",
                    "Identity, authorization, session recovery, and protection expectations.",
                    false,
                    2,
                    "Authentication and access",
                    true,
                    List.of(
                            new Criterion(
                                    "identity",
                                    "Account creation and supported sign-in methods.",
                                    "How should users create an identity and sign in?"),
                            new Criterion(
                                    "authorization",
                                    "Role and resource-level authorization rules.",
                                    "Which protected actions or resources require different permissions?"),
                            new Criterion(
                                    "session_recovery",
                                    "Session duration, sign-out, credential recovery, and account recovery.",
                                    "What session and account-recovery behaviour is required?"),
                            new Criterion(
                                    "security_constraints",
                                    "Relevant privacy, sensitive-data, and stronger-authentication constraints.",
                                    "Which security or privacy constraint must authentication enforce?"))),
            new Definition(
                    RequirementCategory.INTEGRATIONS,
                    "Integrations",
                    "External systems, exchanged data, contracts, authentication, and failure limits.",
                    false,
                    2,
                    "Integrations",
                    true,
                    List.of(
                            new Criterion(
                                    "external_systems",
                                    "External services or systems and the purpose of each connection.",
                                    "Which external system must be connected, and why?"),
                            new Criterion(
                                    "data_exchange",
                                    "Data direction, payload, trigger, and expected result.",
                                    "What data should be exchanged with each external system, and when?"),
                            new Criterion(
                                    "contract_security",
                                    "Protocol, API contract, authentication, and secret-handling constraints.",
                                    "What contract and authentication constraints apply to the integration?"),
                            new Criterion(
                                    "failure_limits",
                                    "Timeout, retry, rate-limit, availability, and degraded-mode behaviour.",
                                    "How should the product behave when an integration is slow or unavailable?"))),
            new Definition(
                    RequirementCategory.ERROR_HANDLING,
                    "Error handling",
                    "Failure scenarios, user communication, recovery, and operational visibility.",
                    false,
                    2,
                    "Error handling",
                    true,
                    List.of(
                            new Criterion(
                                    "failure_scenarios",
                                    "Important invalid-input, dependency, concurrency, and system failure scenarios.",
                                    "Which failure scenario would cause the most harm if it were handled badly?"),
                            new Criterion(
                                    "user_response",
                                    "Safe, useful user-facing behaviour and messages.",
                                    "What should the user see or be able to do when that failure occurs?"),
                            new Criterion(
                                    "recovery",
                                    "Retry, rollback, idempotency, fallback, or manual recovery behaviour.",
                                    "How should the product recover from the most important failure?"),
                            new Criterion(
                                    "observability",
                                    "Logging, monitoring, correlation, and support information.",
                                    "What information must be recorded so important failures can be diagnosed?"))),
            new Definition(
                    RequirementCategory.TESTING,
                    "Testing",
                    "Acceptance criteria, critical journeys, test scope, and measurable quality thresholds.",
                    false,
                    2,
                    "Testing expectations",
                    true,
                    List.of(
                            new Criterion(
                                    "acceptance_criteria",
                                    "Measurable criteria that determine whether the product is acceptable.",
                                    "Which measurable condition must be satisfied before the product is accepted?"),
                            new Criterion(
                                    "critical_journeys",
                                    "Critical workflows and failure paths that must be verified.",
                                    "Which user journey or failure path must never be released without testing?"),
                            new Criterion(
                                    "test_scope",
                                    "Required test levels, environments, data, or compatibility coverage.",
                                    "What test scope or environment is required before release?"),
                            new Criterion(
                                    "quality_thresholds",
                                    "Required performance, reliability, security, or accessibility thresholds.",
                                    "Which quality threshold must be measured before release?"))),
            new Definition(
                    RequirementCategory.DEPLOYMENT,
                    "Deployment",
                    "Target environments, release process, configuration, operations, and rollback.",
                    false,
                    2,
                    "Deployment",
                    true,
                    List.of(
                            new Criterion(
                                    "environments",
                                    "Hosting target and required development, test, staging, or production environments.",
                                    "Where will the product run, and which environments are required?"),
                            new Criterion(
                                    "release_process",
                                    "Build, approval, migration, and release automation expectations.",
                                    "How should a new version move from source code into production?"),
                            new Criterion(
                                    "configuration",
                                    "Environment configuration, secrets, and infrastructure dependencies.",
                                    "How should environment-specific configuration and secrets be supplied?"),
                            new Criterion(
                                    "operations",
                                    "Monitoring, backup, scaling, incident response, and rollback expectations.",
                                    "What operational or rollback capability is required after deployment?"))),
            new Definition(
                    RequirementCategory.PROJECT_TITLE,
                    "Project title",
                    "Final product name chosen from AI suggestions or your own wording.",
                    true,
                    1,
                    "Project title",
                    false,
                    List.of()),
            new Definition(
                    RequirementCategory.OVERALL_IDEA,
                    "Overall idea",
                    "A short product description aligned with gathered requirements, without repeating them.",
                    true,
                    1,
                    "Summary",
                    false,
                    List.of()));

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
