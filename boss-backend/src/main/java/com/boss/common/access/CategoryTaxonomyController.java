package com.boss.common.access;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Static reference data (the fixed sidebar taxonomy) - the single source of truth the
 * frontend renders the sidebar and the user-access checkbox tree from, so a category's
 * label/subcategories never has to be duplicated and kept in sync by hand.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
public class CategoryTaxonomyController {

    @GetMapping
    @Operation(summary = "The fixed category/subcategory taxonomy used for sidebar menu access")
    public ApiResponse<List<CategoryTaxonomyDto>> list() {
        List<CategoryTaxonomyDto> taxonomy = Arrays.stream(Category.values())
                .map(category -> new CategoryTaxonomyDto(category, category.label(), subcategoriesOf(category)))
                .toList();
        return ApiResponse.of(taxonomy, MDC.get(LogFields.CORRELATION_ID));
    }

    private List<SubcategoryTaxonomyDto> subcategoriesOf(Category category) {
        return Arrays.stream(Subcategory.values())
                .filter(subcategory -> subcategory.category() == category)
                .map(subcategory -> new SubcategoryTaxonomyDto(subcategory, subcategory.label()))
                .toList();
    }
}
