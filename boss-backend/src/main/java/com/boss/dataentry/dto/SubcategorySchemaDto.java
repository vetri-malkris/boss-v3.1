package com.boss.dataentry.dto;

import com.boss.dataentry.domain.SubcategorySchema;

import java.util.List;

public record SubcategorySchemaDto(String subcategory, List<FieldDefinitionDto> fields) {

    public static SubcategorySchemaDto from(SubcategorySchema schema) {
        return new SubcategorySchemaDto(
                schema.subcategory().name(),
                schema.fields().stream().map(FieldDefinitionDto::from).toList());
    }
}
