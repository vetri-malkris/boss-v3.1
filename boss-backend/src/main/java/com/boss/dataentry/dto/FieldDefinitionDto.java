package com.boss.dataentry.dto;

import com.boss.dataentry.domain.FieldDefinition;

import java.util.List;

public record FieldDefinitionDto(String name, String label, String type, boolean required, List<String> options) {

    public static FieldDefinitionDto from(FieldDefinition field) {
        return new FieldDefinitionDto(field.name(), field.label(), field.type().name(), field.required(), field.options());
    }
}
