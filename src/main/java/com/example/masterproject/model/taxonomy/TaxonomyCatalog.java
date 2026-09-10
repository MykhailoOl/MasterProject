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
                    "What you want",
                    "The problem you want to fix, the better result you hope for, and what success looks like.",
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
                    "Who will use it",
                    "The people it serves, whether anyone else helps, and who may do what.",
                    true,
                    5,
                    "Users and roles",
                    true,
                    List.of(
                            new Criterion(
                                    "customers",
                                    "The main people being served by the product.",
                                    "Who would you like to use this?",
                                    "Parents and gift shoppers looking for toys in local stores."),
                            new Criterion(
                                    "operators",
                                    "Whether anyone else needs to help with the main tasks; do not assume a business or staff.",
                                    "Would anyone else need to help with the tasks you described?",
                                    "Store clerks who update stock and answer customer questions."),
                            new Criterion(
                                    "managers",
                                    "Whether anyone needs control over who may use the product; do not invent an administrator.",
                                    "Does anyone need to decide who is allowed to use it?",
                                    "The store owner and one store manager."),
                            new Criterion(
                                    "permissions",
                                    "What each group may do or must be blocked from doing.",
                                    "For the people you named, what should each group be allowed or blocked from doing?",
                                    "Customers browse stock; staff update stock; only managers change prices and add staff."),
                            new Criterion(
                                    "usage_context",
                                    "When, where, and how often each group uses the product.",
                                    "When and where do the main people usually use this product?",
                                    "Customers on phones while shopping; staff on a tablet at the counter."))),
            new Definition(
                    RequirementCategory.CORE_FEATURES,
                    "What it should do",
                    "The main things people must be able to do in the first version, and how those tasks flow.",
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
                                    "What should happen from the moment someone starts the main task until they finish?",
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
                    "How people open it",
                    "Website, phone, or computer, which devices matter, and any business limits on tools or quality.",
                    true,
                    5,
                    "Platform and delivery",
                    true,
                    List.of(
                            new Criterion(
                                    "delivery_channel",
                                    "Whether people use a website, phone app, desktop app, or another channel.",
                                    "How should people open and use the product: website, phone app, desktop, or something else?",
                                    "A mobile-friendly website first; no native app in version one."),
                            new Criterion(
                                    "supported_environments",
                                    "Required phones, computers, browsers, or places where it must work.",
                                    "Which phones, computers, or browsers must work from day one?",
                                    "Current Chrome, Safari, and Edge on phones and laptops."),
                            new Criterion(
                                    "technology_constraints",
                                    "Business rules that require or forbid certain tools or systems.",
                                    "Are any tools, systems, or technologies required or forbidden by the business?",
                                    "Must work with the existing store inventory spreadsheet export."),
                            new Criterion(
                                    "quality_constraints",
                                    "Speed, offline use, accessibility, or uptime expectations that matter most.",
                                    "Which quality need matters most: speed, offline use, accessibility, or uptime?",
                                    "Search results should appear in under two seconds on a normal phone connection."))),
            new Definition(
                    RequirementCategory.NON_GOALS,
                    "What to leave out",
                    "Things that should stay out of the first version so the plan stays focused.",
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
                                    "Is there anything the person building this might wrongly assume you want?",
                                    "Each shop already keeps a daily stock list that can be uploaded."))),
            new Definition(
                    RequirementCategory.DATA_ENTITIES,
                    "Information to remember",
                    "What the product must keep track of, how pieces connect, and who may change them.",
                    false,
                    5,
                    "Information the product keeps",
                    true,
                    List.of(
                            new Criterion(
                                    "entities_attributes",
                                    "Core information the product must remember.",
                                    "What information must the product remember to do its job?",
                                    "Stores, toys, stock counts, reservations, and staff accounts."),
                            new Criterion(
                                    "relationships",
                                    "How those pieces of information connect.",
                                    "How are those pieces of information connected to each other?",
                                    "One store has many toys; one reservation belongs to one customer and one toy."),
                            new Criterion(
                                    "ownership_access",
                                    "Who may view or change each important piece of information.",
                                    "Who may view or change each important piece of stored information?",
                                    "Staff update stock for their store; managers can edit any store; customers see only public stock."),
                            new Criterion(
                                    "lifecycle",
                                    "How long records stay and when they may change or be removed.",
                                    "How long should key records be kept, and when may they be changed or removed?",
                                    "Reservations expire after 24 hours; stock history is kept for 90 days."))),
            new Definition(
                    RequirementCategory.AUTHENTICATION,
                    "Sign-in and access",
                    "Who needs an account, what stronger permissions exist, and what must stay private.",
                    false,
                    5,
                    "Sign-in and access",
                    true,
                    List.of(
                            new Criterion(
                                    "identity",
                                    "Who needs an account and how people sign in.",
                                    "Should people need to identify themselves before using the private parts?",
                                    "Customers can browse without an account; staff sign in with email and password."),
                            new Criterion(
                                    "authorization",
                                    "Which actions need stronger permission than a normal signed-in person.",
                                    "Should any action be available only to particular people?",
                                    "Only managers can create staff accounts or change prices."),
                            new Criterion(
                                    "session_recovery",
                                    "How long people stay signed in and how they recover a lost password.",
                                    "What should someone be able to do if they lose access?",
                                    "Staff stay signed in for one workday; password reset uses email."),
                            new Criterion(
                                    "security_constraints",
                                    "Privacy or safety rules that sign-in must protect.",
                                    "What security or privacy rule must sign-in protect?",
                                    "Customer phone numbers are hidden from other customers and from junior staff."))),
            new Definition(
                    RequirementCategory.INTEGRATIONS,
                    "Connections to other tools",
                    "Outside systems the product must talk to, what moves between them, and what happens if they fail.",
                    false,
                    5,
                    "Connections to other systems",
                    true,
                    List.of(
                            new Criterion(
                                    "external_systems",
                                    "Outside systems and why the product must connect to them.",
                                    "Which outside system must this product connect to, and why?",
                                    "Nightly stock files from the existing inventory spreadsheet tool."),
                            new Criterion(
                                    "data_exchange",
                                    "What data moves, when it moves, and what result is expected.",
                                    "What data moves between systems, and when does that happen?",
                                    "Each night the store sends toy IDs and stock counts; the product updates availability."),
                            new Criterion(
                                    "contract_security",
                                    "How the connection should be limited or protected.",
                                    "Is there information that must never be shared with that service?",
                                    "Only the store system may upload stock using a private upload key."),
                            new Criterion(
                                    "failure_limits",
                                    "What should happen when the outside system is late or unavailable.",
                                    "What should happen if that outside system is late or unavailable?",
                                    "Keep yesterday's stock visible and mark it as last updated overnight."))),
            new Definition(
                    RequirementCategory.ERROR_HANDLING,
                    "When something goes wrong",
                    "Important failures, what people should see, and how the product should recover.",
                    false,
                    5,
                    "Error handling",
                    true,
                    List.of(
                            new Criterion(
                                    "failure_scenarios",
                                    "Important failures that would hurt people if handled badly.",
                                    "Which failure would hurt people most if the product handled it badly?",
                                    "Two customers reserve the last toy at the same time."),
                            new Criterion(
                                    "user_response",
                                    "What the person should see or do when that failure happens.",
                                    "What should the person see or be able to do when that failure happens?",
                                    "Show that the toy just sold out and offer the next nearest store."),
                            new Criterion(
                                    "recovery",
                                    "How the product should recover so people and records stay safe.",
                                    "How should the product recover so data and people stay safe?",
                                    "Only one reservation is kept; the second attempt is rejected cleanly."),
                            new Criterion(
                                    "observability",
                                    "What should be recorded so staff can understand and fix important failures.",
                                    "What would help someone understand what went wrong?",
                                    "Log the toy, store, and both reservation attempts with timestamps."))),
            new Definition(
                    RequirementCategory.TESTING,
                    "How you will check it",
                    "Simple checks that prove the first version works well enough before you accept it.",
                    false,
                    5,
                    "Testing expectations",
                    true,
                    List.of(
                            new Criterion(
                                    "acceptance_criteria",
                                    "Observable criteria that determine whether the product is acceptable; numbers only when meaningful to the owner.",
                                    "What would you try to check that the first version does what you need?",
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
                                    "Is there a situation where this would feel too slow or difficult to use?",
                                    "Search must return results in under two seconds on a mid-range phone."))),
            new Definition(
                    RequirementCategory.DEPLOYMENT,
                    "Going live",
                    "Where the live product runs, how updates go live, and what backup or undo you need.",
                    false,
                    5,
                    "Going live and updates",
                    true,
                    List.of(
                            new Criterion(
                                    "environments",
                                    "Where the live product runs and whether a separate test copy is needed.",
                                    "Would you like to try a private version before other people can use it?",
                                    "One test site and one live site hosted on a standard cloud host."),
                            new Criterion(
                                    "release_process",
                                    "How a finished update moves into the live site.",
                                    "Is there anything people need to know before the product changes?",
                                    "Deploy to test first, then promote to live after a short checklist."),
                            new Criterion(
                                    "configuration",
                                    "Any known restrictions on where the product or its information may be kept; leave implementation open.",
                                    "Do you have any restrictions on where your information may be kept?",
                                    "Database and upload keys stay in host environment settings, not in code."),
                            new Criterion(
                                    "operations",
                                    "Backup, alerts, or undo needs after go-live.",
                                    "If an update goes wrong, what would people need to get back?",
                                    "Daily database backups and a one-step rollback to the previous release."))),
            new Definition(
                    RequirementCategory.PROJECT_TITLE,
                    "Project name",
                    "A clear name for your product, chosen from suggestions or written by you.",
                    true,
                    1,
                    "Project title",
                    false,
                    List.of()),
            new Definition(
                    RequirementCategory.OVERALL_IDEA,
                    "Short summary",
                    "A short product description that matches what you answered, without repeating every topic.",
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

    public static boolean isBlocking(RequirementCategory category, String criterionId) {
        if (criterionId == null || criterionId.isBlank()) {
            return false;
        }
        return blockingIds(category).contains(criterionId);
    }

    public static List<String> blockingIds(RequirementCategory category) {
        return switch (category) {
            case GOAL -> List.of("problem", "outcome");
            case USERS_AND_ROLES -> List.of("customers", "operators");
            case CORE_FEATURES -> List.of("capabilities", "workflow");
            case PLATFORM -> List.of("delivery_channel");
            case NON_GOALS -> List.of("excluded_capabilities");
            case DATA_ENTITIES -> List.of("entities_attributes");
            case AUTHENTICATION -> List.of("identity");
            case INTEGRATIONS -> List.of("external_systems");
            case ERROR_HANDLING -> List.of("failure_scenarios");
            case TESTING -> List.of("acceptance_criteria");
            case DEPLOYMENT -> List.of("environments");
            case PROJECT_TITLE, OVERALL_IDEA -> List.of();
        };
    }
}
