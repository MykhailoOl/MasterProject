package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GuidedElicitationPlannerTests {

    private final GuidedElicitationPlanner planner =
            new GuidedElicitationPlanner(new ObjectMapper());

    @Test
    void completesFoundationalCategoriesBeforeOptionalCategories() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 0, 2);
        ProjectCategory users = category(RequirementCategory.USERS_AND_ROLES, true, 0, 2);
        ProjectCategory features = category(RequirementCategory.CORE_FEATURES, true, 0, 4);
        ProjectCategory platform = category(RequirementCategory.PLATFORM, true, 0, 2);
        ProjectCategory integrations = category(RequirementCategory.INTEGRATIONS, false, 0, 2);

        var selected = planner.nextCategory(
                List.of(goal, users, features, platform, integrations),
                List.of(
                        slot(RequirementCategory.GOAL, 0.5),
                        slot(RequirementCategory.USERS_AND_ROLES, 0.75),
                        slot(RequirementCategory.CORE_FEATURES, 0.75),
                        slot(RequirementCategory.PLATFORM, 0.75),
                        slot(RequirementCategory.INTEGRATIONS, 0.0)));

        assertThat(selected).contains(goal);
    }

    @Test
    void selectsLargestRemainingGapAfterFoundationsAreUsable() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 1, 2);
        ProjectCategory users = category(RequirementCategory.USERS_AND_ROLES, true, 1, 2);
        ProjectCategory integrations = category(RequirementCategory.INTEGRATIONS, false, 0, 2);

        var selected = planner.nextCategory(
                List.of(goal, users, integrations),
                List.of(
                        slot(RequirementCategory.GOAL, 0.75),
                        slot(RequirementCategory.USERS_AND_ROLES, 0.75),
                        slot(RequirementCategory.INTEGRATIONS, 0.0)));

        assertThat(selected).contains(integrations);
    }

    @Test
    void skipsCoveredBodyCategoriesAndLeavesClosingQuestionsUntilLast() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 0, 2);
        ProjectCategory title = category(RequirementCategory.PROJECT_TITLE, true, 0, 1);

        var selected = planner.nextCategory(
                List.of(goal, title),
                List.of(
                        slot(RequirementCategory.GOAL, 1.0),
                        slot(RequirementCategory.PROJECT_TITLE, 0.0)));

        assertThat(selected).contains(title);
    }

    @Test
    void targetsAnUnaskedMissingCriterionBeforeRepeatingAQuestion() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.GOAL);

        TaxonomyCatalog.Criterion selected = planner.nextCriterion(
                definition,
                """
                {"problem":"MISSING","outcome":"MISSING","success":"PARTIAL","priority":"COVERED"}
                """,
                Set.of("problem"));

        assertThat(selected.id()).isEqualTo("outcome");
    }

    @Test
    void usersAndRolesPreferUnaskedCustomerStaffAndManagerCriteria() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.USERS_AND_ROLES);

        TaxonomyCatalog.Criterion selected = planner.nextCriterion(
                definition,
                """
                {"customers":"COVERED","operators":"MISSING","managers":"MISSING","permissions":"MISSING","usage_context":"MISSING"}
                """,
                Set.of());

        assertThat(selected.id()).isEqualTo("operators");
    }

    private ProjectCategory category(
            RequirementCategory category,
            boolean mandatory,
            int questionsAsked,
            int maxQuestions) {
        ProjectCategory row = new ProjectCategory();
        row.setCategory(category);
        row.setMandatory(mandatory);
        row.setQuestionsAsked(questionsAsked);
        row.setMaxQuestions(maxQuestions);
        return row;
    }

    private RequirementSlot slot(RequirementCategory category, double completeness) {
        RequirementSlot slot = new RequirementSlot();
        slot.setCategory(category);
        slot.setCompleteness(completeness);
        return slot;
    }
}
