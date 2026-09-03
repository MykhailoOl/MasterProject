package com.example.masterproject.model.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TaxonomyCatalogTests {

    @Test
    void everySpecificationCategoryHasDistinctCriteriaAndFallbackQuestions() {
        TaxonomyCatalog.all().stream()
                .filter(TaxonomyCatalog.Definition::includeInSpecBody)
                .forEach(definition -> {
                    assertThat(definition.criteria()).hasSizeGreaterThanOrEqualTo(4);
                    assertThat(definition.criteria())
                            .allSatisfy(criterion -> {
                                assertThat(criterion.id()).isNotBlank();
                                assertThat(criterion.description()).isNotBlank();
                                assertThat(criterion.fallbackQuestion()).isNotBlank();
                            });
                    assertThat(definition.criteria().stream()
                                    .map(TaxonomyCatalog.Criterion::id)
                                    .toList())
                            .doesNotHaveDuplicates();
                });
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
