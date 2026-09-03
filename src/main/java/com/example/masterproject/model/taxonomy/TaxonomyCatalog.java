package com.example.masterproject.model.taxonomy;

import com.example.masterproject.model.enums.RequirementCategory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class TaxonomyCatalog {

    public record Criterion(
            String id,
            String description,
            String fallbackQuestion,
            String answerExample) {
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
                    5,
                    "Goals",
                    true,
                    List.of(
                            new Criterion(
                                    "problem",
                                    "The specific problem or current difficulty to solve.",
                                    "What concrete problem or frustration should the first version remove for people?",
                                    "Parents waste evenings searching three shops because toy stock is unclear."),
                            new Criterion(
                                    "outcome",
                                    "The outcome or benefit users should achieve.",
                                    "What better result should people get after using this product?",
                                    "A parent finds an in-stock toy nearby in under two minutes."),
                            new Criterion(
                                    "success",
                                    "An observable or measurable signal of success.",
                                    "What visible result would prove the product is working well enough?",
                                    "At least 70% of searches end with a confirmed in-stock item."),
                            new Criterion(
                                    "priority",
                                    "The most important outcome when trade-offs are necessary.",
                                    "If only one outcome can be protected first, which one matters most?",
                                    "Correct stock information matters more than fancy recommendations."))),
            new Definition(
                    RequirementCategory.USERS_AND_ROLES,
                    "Users and roles",
                    "Customers, staff, managers, and what each group may do.",
                    true,
                    5,
                    "Users and roles",
                    true,
                    List.of(
                            new Criterion(
                                    "customers",
                                    "The main people being served by the product.",
                                    "Who is this product mainly for as the everyday customer or visitor?",
                                    "Parents and gift shoppers looking for toys in local stores."),
                            new Criterion(
                                    "operators",
                                    "People who run day-to-day work in the business.",
                                    "Besides customers, who in the business will use it during a normal workday?",
                                    "Store clerks who update stock and answer customer questions."),
                            new Criterion(
                                    "managers",
                                    "People who control settings, accounts, or sensitive actions.",
                                    "Who needs stronger control, such as pricing, inventory overrides, or staff accounts?",
                                    "The store owner and one store manager."),
                            new Criterion(
                                    "permissions",
                                    "What each group may do or must be blocked from doing.",
                                    "What should customers, staff, and managers each be allowed or blocked from doing?",
                                    "Customers browse stock; staff update stock; only managers change prices and add staff."),
                            new Criterion(
                                    "usage_context",
                                    "When, where, and how often each group uses the product.",
                                    "When and where do the main people usually use this product?",
                                    "Customers on phones while shopping; staff on a tablet at the counter."))),
            new Definition(
                    RequirementCategory.CORE_FEATURES,
                    "Core features",
                    "Prioritized capabilities, workflows, inputs, outputs, and acceptance behaviour.",
                    true,
                    5,
                    "Core features",
                    true,
                    List.of(
                            new Criterion(
                                    "capabilities",
                                    "The essential capabilities required in the first usable release.",
                                    "What must someone be able to do in the first usable version?",
                                    "Search toys by name, see nearby stock, and reserve one item for pickup."),
                            new Criterion(
                                    "workflow",
                                    "The main user workflow, including its trigger and sequence.",
                                    "Walk through the main task from the moment it starts to the moment it finishes.",
                                    "Customer searches a toy, picks a store, reserves it, then collects it in store."),
                            new Criterion(
                                    "inputs_outputs",
                                    "Important inputs, outputs, and state changes.",
                                    "What information goes in during that main task, and what result comes out?",
                                    "Input: toy name and city. Output: matching stores, stock count, and a reservation code."),
                            new Criterion(
                                    "acceptance",
                                    "Observable acceptance behaviour and important exceptions.",
                                    "How can someone tell the main task succeeded, and what should happen when it fails?",
                                    "Success shows a reservation code; if stock runs out, the customer is told immediately."))),
            new Definition(
                    RequirementCategory.PLATFORM,
                    "Platform",
                    "Delivery channel, supported environments, technical constraints, and quality expectations.",
                    true,
                    5,
                    "Platform and stack",
                    true,
                    List.of(
                            new Criterion(
                                    "delivery_channel",
                                    "Whether the product is web, mobile, desktop, API, embedded, or another channel.",
                                    "How should people open and use the product: website, phone app, desktop, or something else?",
                                    "A mobile-friendly website first; no native app in version one."),
                            new Criterion(
                                    "supported_environments",
                                    "Required devices, operating systems, browsers, or runtime environments.",
                                    "Which phones, computers, or browsers must work from day one?",
                                    "Current Chrome, Safari, and Edge on phones and laptops."),
                            new Criterion(
                                    "technology_constraints",
                                    "Required or prohibited technologies and compatibility constraints.",
                                    "Are any tools, systems, or technologies required or forbidden by the business?",
                                    "Must work with the existing store inventory spreadsheet export."),
                            new Criterion(
                                    "quality_constraints",
                                    "Platform-level performance, availability, accessibility, or offline expectations.",
                                    "Which quality need matters most: speed, offline use, accessibility, or uptime?",
                                    "Search results should appear in under two seconds on a normal phone connection."))),
            new Definition(
                    RequirementCategory.NON_GOALS,
                    "Non-goals",
                    "Explicit product boundaries, exclusions, deferred work, and assumptions.",
                    false,
                    5,
                    "Non-goals",
                    true,
                    List.of(
                            new Criterion(
                                    "excluded_capabilities",
                                    "Capabilities explicitly excluded from the first release.",
                                    "What related features must stay out of the first version on purpose?",
                                    "No home delivery and no online payment in version one."),
                            new Criterion(
                                    "system_boundary",
                                    "Where this product's responsibility starts and ends.",
                                    "Where should this product stop, and what stays outside its job?",
                                    "It shows stock and reservations; in-store checkout stays with the cash register."),
                            new Criterion(
                                    "deferred_work",
                                    "Ideas deliberately deferred to a later release.",
                                    "Which good ideas should wait until after the first release?",
                                    "Wish lists and personalized recommendations wait for later."),
                            new Criterion(
                                    "assumptions",
                                    "Important assumptions that prevent accidental scope expansion.",
                                    "What assumption about the first release should be written down so nobody expands scope by accident?",
                                    "Each shop already keeps a daily stock list that can be uploaded."))),
            new Definition(
                    RequirementCategory.DATA_ENTITIES,
                    "Data entities",
                    "Core information, relationships, ownership, and lifecycle rules.",
                    false,
                    5,
                    "Data model",
                    true,
                    List.of(
                            new Criterion(
                                    "entities_attributes",
                                    "Core entities and the information each must retain.",
                                    "What information must the product remember to do its job?",
                                    "Stores, toys, stock counts, reservations, and staff accounts."),
                            new Criterion(
                                    "relationships",
                                    "Relationships and cardinality between core entities.",
                                    "How are those pieces of information connected to each other?",
                                    "One store has many toys; one reservation belongs to one customer and one toy."),
                            new Criterion(
                                    "ownership_access",
                                    "Who owns, creates, reads, and changes the data.",
                                    "Who may view or change each important piece of stored information?",
                                    "Staff update stock for their store; managers can edit any store; customers see only public stock."),
                            new Criterion(
                                    "lifecycle",
                                    "Creation, update, deletion, retention, and audit rules.",
                                    "How long should key records be kept, and when may they be changed or removed?",
                                    "Reservations expire after 24 hours; stock history is kept for 90 days."))),
            new Definition(
                    RequirementCategory.AUTHENTICATION,
                    "Authentication",
                    "Identity, authorization, session recovery, and protection expectations.",
                    false,
                    5,
                    "Authentication and access",
                    true,
                    List.of(
                            new Criterion(
                                    "identity",
                                    "Account creation and supported sign-in methods.",
                                    "Who needs an account, and how should they sign in?",
                                    "Customers can browse without an account; staff sign in with email and password."),
                            new Criterion(
                                    "authorization",
                                    "Role and resource-level authorization rules.",
                                    "Which actions need stronger permission than a normal signed-in user?",
                                    "Only managers can create staff accounts or change prices."),
                            new Criterion(
                                    "session_recovery",
                                    "Session duration, sign-out, credential recovery, and account recovery.",
                                    "How long should people stay signed in, and how do they recover a lost password?",
                                    "Staff stay signed in for one workday; password reset uses email."),
                            new Criterion(
                                    "security_constraints",
                                    "Relevant privacy, sensitive-data, and stronger-authentication constraints.",
                                    "What security or privacy rule must sign-in protect?",
                                    "Customer phone numbers are hidden from other customers and from junior staff."))),
            new Definition(
                    RequirementCategory.INTEGRATIONS,
                    "Integrations",
                    "External systems, exchanged data, contracts, authentication, and failure limits.",
                    false,
                    5,
                    "Integrations",
                    true,
                    List.of(
                            new Criterion(
                                    "external_systems",
                                    "External services or systems and the purpose of each connection.",
                                    "Which outside system must this product connect to, and why?",
                                    "Nightly stock files from the existing inventory spreadsheet tool."),
                            new Criterion(
                                    "data_exchange",
                                    "Data direction, payload, trigger, and expected result.",
                                    "What data moves between systems, and when does that happen?",
                                    "Each night the store sends toy IDs and stock counts; the product updates availability."),
                            new Criterion(
                                    "contract_security",
                                    "Protocol, API contract, authentication, and secret-handling constraints.",
                                    "How should that connection be secured or limited?",
                                    "Only the store system may upload stock using a private upload key."),
                            new Criterion(
                                    "failure_limits",
                                    "Timeout, retry, rate-limit, availability, and degraded-mode behaviour.",
                                    "What should happen if that outside system is late or unavailable?",
                                    "Keep yesterday's stock visible and mark it as last updated overnight."))),
            new Definition(
                    RequirementCategory.ERROR_HANDLING,
                    "Error handling",
                    "Failure scenarios, user communication, recovery, and operational visibility.",
                    false,
                    5,
                    "Error handling",
                    true,
                    List.of(
                            new Criterion(
                                    "failure_scenarios",
                                    "Important invalid-input, dependency, concurrency, and system failure scenarios.",
                                    "Which failure would hurt people most if the product handled it badly?",
                                    "Two customers reserve the last toy at the same time."),
                            new Criterion(
                                    "user_response",
                                    "Safe, useful user-facing behaviour and messages.",
                                    "What should the person see or be able to do when that failure happens?",
                                    "Show that the toy just sold out and offer the next nearest store."),
                            new Criterion(
                                    "recovery",
                                    "Retry, rollback, idempotency, fallback, or manual recovery behaviour.",
                                    "How should the product recover so data and people stay safe?",
                                    "Only one reservation is kept; the second attempt is rejected cleanly."),
                            new Criterion(
                                    "observability",
                                    "Logging, monitoring, correlation, and support information.",
                                    "What should be recorded so staff can understand and fix important failures?",
                                    "Log the toy, store, and both reservation attempts with timestamps."))),
            new Definition(
                    RequirementCategory.TESTING,
                    "Testing",
                    "Acceptance criteria, critical journeys, test scope, and measurable quality thresholds.",
                    false,
                    5,
                    "Testing expectations",
                    true,
                    List.of(
                            new Criterion(
                                    "acceptance_criteria",
                                    "Measurable criteria that determine whether the product is acceptable.",
                                    "What measurable check must pass before you accept the first version?",
                                    "A customer can reserve an in-stock toy and staff can see that reservation."),
                            new Criterion(
                                    "critical_journeys",
                                    "Critical workflows and failure paths that must be verified.",
                                    "Which journey or failure case must be tested every time before release?",
                                    "Search, reserve, and the sold-out conflict path."),
                            new Criterion(
                                    "test_scope",
                                    "Required test levels, environments, data, or compatibility coverage.",
                                    "What kinds of tests or devices are required before release?",
                                    "Manual checks on phone and laptop using sample stock from two stores."),
                            new Criterion(
                                    "quality_thresholds",
                                    "Required performance, reliability, security, or accessibility thresholds.",
                                    "Which quality number must be measured before release?",
                                    "Search must return results in under two seconds on a mid-range phone."))),
            new Definition(
                    RequirementCategory.DEPLOYMENT,
                    "Deployment",
                    "Target environments, release process, configuration, operations, and rollback.",
                    false,
                    5,
                    "Deployment",
                    true,
                    List.of(
                            new Criterion(
                                    "environments",
                                    "Hosting target and required development, test, staging, or production environments.",
                                    "Where will the live product run, and do you need a separate test copy?",
                                    "One test site and one live site hosted on a standard cloud host."),
                            new Criterion(
                                    "release_process",
                                    "Build, approval, migration, and release automation expectations.",
                                    "How should a new version move from finished work into the live site?",
                                    "Deploy to test first, then promote to live after a short checklist."),
                            new Criterion(
                                    "configuration",
                                    "Environment configuration, secrets, and infrastructure dependencies.",
                                    "How should secrets and environment settings be supplied safely?",
                                    "Database and upload keys stay in host environment settings, not in code."),
                            new Criterion(
                                    "operations",
                                    "Monitoring, backup, scaling, incident response, and rollback expectations.",
                                    "What must be ready after go-live for backup, alerts, or undo?",
                                    "Daily database backups and a one-step rollback to the previous release."))),
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

    public static Optional<Criterion> criterion(RequirementCategory category, String criterionId) {
        if (criterionId == null || criterionId.isBlank()) {
            return Optional.empty();
        }
        return require(category).criteria().stream()
                .filter(criterion -> criterion.id().equals(criterionId))
                .findFirst();
    }

    public static boolean isKnown(RequirementCategory category) {
        return Arrays.stream(RequirementCategory.values()).anyMatch(value -> value == category);
    }

    public static boolean isClosing(RequirementCategory category) {
        return category == RequirementCategory.PROJECT_TITLE
                || category == RequirementCategory.OVERALL_IDEA;
    }
}
