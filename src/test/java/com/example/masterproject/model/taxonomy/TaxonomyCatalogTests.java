package com.example.masterproject.model.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.masterproject.model.enums.RequirementCategory;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TaxonomyCatalogTests {

    @Test
    void everySpecificationCategoryHasDistinctCriteriaAndFallbackQuestions() {
        TaxonomyCatalog.all().stream()
                .filter(TaxonomyCatalog.Definition::includeInSpecBody)
                .forEach(definition -> {
                    assertThat(definition.maxQuestions()).isGreaterThanOrEqualTo(5);
                    assertThat(definition.criteria()).hasSizeGreaterThanOrEqualTo(4);
                    assertThat(definition.criteria())
                            .allSatisfy(criterion -> {
                                assertThat(criterion.id()).isNotBlank();
                                assertThat(criterion.description()).isNotBlank();
                                assertThat(criterion.fallbackQuestion()).isNotBlank();
                                assertThat(criterion.answerExample()).isNotBlank();
                            });
                    assertThat(definition.criteria().stream()
                                    .map(TaxonomyCatalog.Criterion::id)
                                    .toList())
                            .doesNotHaveDuplicates();
                });
    }

    @Test
    void usersAndRolesCoverCustomersStaffAndManagersSeparately() {
        assertThat(TaxonomyCatalog.require(RequirementCategory.USERS_AND_ROLES).criteria())
                .extracting(TaxonomyCatalog.Criterion::id)
                .containsExactly("customers", "operators", "managers", "permissions", "usage_context");
    }

    @Test
    void criterionIdentifiersAreUniqueWithinEachCategoryAndFitDatabaseColumn() {
        TaxonomyCatalog.all().forEach(definition -> {
            HashSet<String> identifiers = new HashSet<>();
            definition.criteria().forEach(criterion -> {
                assertThat(criterion.id()).hasSizeLessThanOrEqualTo(64);
                assertThat(identifiers.add(criterion.id())).isTrue();
            });
        });
    }
}
