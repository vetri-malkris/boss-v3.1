package com.boss.common.access;

import java.util.List;

public record CategoryTaxonomyDto(Category code, String label, List<SubcategoryTaxonomyDto> subcategories) {
}
