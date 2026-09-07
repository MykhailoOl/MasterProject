package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GuidedElicitationPlannerTests {

    private final GuidedElicitationPlanner planner =
            new GuidedElicitationPlanner(new ObjectMapper());

    @Test
    void staysOnTheCurrentTaxonomyInsteadOfHoppingToAWeakerOne() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 1, 5);
        ProjectCategory users = category(RequirementCategory.USERS_AND_ROLES, true, 0, 5);

        var selected = planner.nextCategory(
                List.of(goal, users),
                List.of(
                        slot(
                                RequirementCategory.GOAL,
                                0.25,
                                """
                                {"problem":"PARTIAL","outcome":"MISSING","success":"MISSING","priority":"MISSING"}
                                """),
                        slot(RequirementCategory.USERS_AND_ROLES, 0.0, null)),
                Map.of(RequirementCategory.GOAL, List.of("problem")));

        assertThat(selected).contains(goal);
    }

    @Test
    void asksMandatoryCoreEvenWhenExtractionLooksCovered() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 0, 5);
        ProjectCategory title = category(RequirementCategory.PROJECT_TITLE, true, 0, 1);

        var selected = planner.nextCategory(
                List.of(goal, title),
                List.of(
                        slot(
                                RequirementCategory.GOAL,
                                1.0,
                                """
                                {"problem":"COVERED","outcome":"COVERED","success":"COVERED","priority":"COVERED"}
                                """),
                        slot(RequirementCategory.PROJECT_TITLE, 0.0, null)),
                Map.of());

        assertThat(selected).contains(goal);
    }

    @Test
    void leavesACoveredCoreAfterTheForcedFirstQuestion() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 1, 5);
        ProjectCategory title = category(RequirementCategory.PROJECT_TITLE, true, 0, 1);

        var selected = planner.nextCategory(
                List.of(goal, title),
                List.of(
                        slot(
                                RequirementCategory.GOAL,
                                1.0,
                                """
                                {"problem":"COVERED","outcome":"COVERED","success":"COVERED","priority":"COVERED"}
                                """),
                        slot(RequirementCategory.PROJECT_TITLE, 0.0, null)),
                Map.of(RequirementCategory.GOAL, List.of("problem")));

        assertThat(selected).contains(title);
    }

    @Test
    void finishesCoreTopicsBeforeOptionalTopics() {
        ProjectCategory goal = category(RequirementCategory.GOAL, true, 1, 5);
        ProjectCategory users = category(RequirementCategory.USERS_AND_ROLES, true, 0, 5);
        ProjectCategory integrations = category(RequirementCategory.INTEGRATIONS, false, 0, 5);

        var selected = planner.nextCategory(
                List.of(goal, users, integrations),
                List.of(
                        slot(
                                RequirementCategory.GOAL,
                                1.0,
                                """
                                {"problem":"COVERED","outcome":"COVERED","success":"COVERED","priority":"COVERED"}
                                """),
                        slot(RequirementCategory.USERS_AND_ROLES, 0.0, null),
                        slot(RequirementCategory.INTEGRATIONS, 0.0, null)),
                Map.of(RequirementCategory.GOAL, List.of("problem")));

        assertThat(selected).contains(users);
    }

    @Test
    void skipsCoveredOptionalTopicsWithoutForcingAQuestion() {
        ProjectCategory integrations = category(RequirementCategory.INTEGRATIONS, false, 0, 5);
        ProjectCategory title = category(RequirementCategory.PROJECT_TITLE, true, 0, 1);

        var selected = planner.nextCategory(
                List.of(integrations, title),
                List.of(
                        slot(
                                RequirementCategory.INTEGRATIONS,
                                1.0,
                                """
                                {"external_systems":"COVERED","data_exchange":"COVERED","contract_security":"COVERED","failure_limits":"COVERED"}
                                """),
                        slot(RequirementCategory.PROJECT_TITLE, 0.0, null)),
                Map.of());

        assertThat(selected).contains(title);
    }

    @Test
    void targetsAnUnaskedMissingCriterionBeforeRepeatingAQuestion() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.GOAL);

        var selected = planner.nextCriterion(
                definition,
                """
                {"problem":"MISSING","outcome":"MISSING","success":"PARTIAL","priority":"COVERED"}
                """,
                List.of("problem"));

        assertThat(selected).isPresent();
        assertThat(selected.get().id()).isEqualTo("outcome");
    }

    @Test
    void doesNotReaskAProbedSecondaryPartial() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.GOAL);

        var selected = planner.nextCriterion(
                definition,
                """
                {"problem":"COVERED","outcome":"COVERED","success":"PARTIAL","priority":"COVERED"}
                """,
                List.of("success"));

        assertThat(selected).isEmpty();
    }

    @Test
    void allowsASecondProbeOnABlockingGapThenStops() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.GOAL);

        var secondProbe = planner.nextCriterion(
                definition,
                """
                {"problem":"PARTIAL","outcome":"COVERED","success":"COVERED","priority":"COVERED"}
                """,
                List.of("problem"));
        var afterBudget = planner.nextCriterion(
                definition,
                """
                {"problem":"PARTIAL","outcome":"COVERED","success":"COVERED","priority":"COVERED"}
                """,
                List.of("problem", "problem"));

        assertThat(secondProbe).isPresent();
        assertThat(secondProbe.get().id()).isEqualTo("problem");
        assertThat(afterBudget).isEmpty();
    }

    @Test
    void skipsGatePrunedCriteria() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.GOAL);

        var selected = planner.nextCriterion(
                definition,
                """
                {"problem":"COVERED","outcome":"COVERED","success":"PARTIAL","priority":"COVERED","_pruned":["success"]}
                """,
                List.of());

        assertThat(selected).isEmpty();
    }

    @Test
    void usersAndRolesPreferUnaskedCustomerStaffAndManagerCriteria() {
        TaxonomyCatalog.Definition definition = TaxonomyCatalog.require(RequirementCategory.USERS_AND_ROLES);

        var selected = planner.nextCriterion(
                definition,
                """
                {"customers":"COVERED","operators":"MISSING","managers":"MISSING","permissions":"MISSING","usage_context":"MISSING"}
                """,
                List.of());

        assertThat(selected).isPresent();
        assertThat(selected.get().id()).isEqualTo("operators");
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

    private RequirementSlot slot(RequirementCategory category, double completeness, String assessmentJson) {
        RequirementSlot slot = new RequirementSlot();
        slot.setCategory(category);
        slot.setCompleteness(completeness);
        slot.setAssessmentJson(assessmentJson);
        return slot;
    }
}
