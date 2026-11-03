package com.boss.dataentry.domain;

import java.util.List;

/**
 * One field in a subcategory's data-entry form - real, distinct, per-subcategory
 * domain metadata (not a generic label/value pair). {@code options} is only
 * populated for {@link FieldType#ENUM} fields.
 */
public record FieldDefinition(String name, String label, FieldType type, boolean required, List<String> options) {

    public FieldDefinition {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static FieldDefinition text(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.TEXT, required, List.of());
    }

    public static FieldDefinition number(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.NUMBER, required, List.of());
    }

    public static FieldDefinition date(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.DATE, required, List.of());
    }

    public static FieldDefinition enumField(String name, String label, boolean required, String... options) {
        return new FieldDefinition(name, label, FieldType.ENUM, required, List.of(options));
    }
}
