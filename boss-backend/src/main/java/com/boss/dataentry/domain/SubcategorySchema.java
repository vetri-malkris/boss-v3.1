package com.boss.dataentry.domain;

import com.boss.common.access.Subcategory;

import java.util.List;

/** The field schema a subcategory's data-entry form and bulk upload are built from. */
public record SubcategorySchema(Subcategory subcategory, List<FieldDefinition> fields) {

    public SubcategorySchema {
        fields = List.copyOf(fields);
    }
}
