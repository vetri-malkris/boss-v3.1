package com.boss.common.access;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTaxonomyControllerTest {

    private final CategoryTaxonomyController controller = new CategoryTaxonomyController();

    @Test
    void returnsEveryCategoryInEnumOrderWithItsLabel() {
        List<CategoryTaxonomyDto> taxonomy = controller.list().data();

        assertThat(taxonomy).hasSize(Category.values().length);
        assertThat(taxonomy.stream().map(CategoryTaxonomyDto::code).toList())
                .containsExactly(Category.values());
        assertThat(taxonomy.stream().map(CategoryTaxonomyDto::label))
                .containsExactly("Human Resource", "Finance", "Accounting", "Auditing", "Compliance", "Risk");
    }

    @Test
    void everySubcategoryAppearsExactlyOnceUnderItsOwnCategory() {
        List<CategoryTaxonomyDto> taxonomy = controller.list().data();

        Set<Subcategory> seen = taxonomy.stream()
                .flatMap(category -> category.subcategories().stream()
                        .peek(sub -> assertThat(sub.code().category()).isEqualTo(category.code())))
                .map(SubcategoryTaxonomyDto::code)
                .collect(Collectors.toSet());

        assertThat(seen).containsExactlyInAnyOrder(Subcategory.values());
    }

    @Test
    void noCategoryIsLeftWithoutAtLeastOneSubcategory() {
        List<CategoryTaxonomyDto> taxonomy = controller.list().data();

        assertThat(taxonomy).allSatisfy(category -> assertThat(category.subcategories()).isNotEmpty());
    }
}
