package com.boss.dataentry.validation;

import com.boss.dataentry.domain.FieldDefinition;
import com.boss.dataentry.domain.SubcategorySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Validates and type-coerces one raw row (a form submission or one parsed CSV/Excel
 * row - both arrive as case-insensitive {@code Map<String,String>}) against a
 * subcategory's schema. Unknown keys not in the schema are dropped; nothing here
 * invents a value - a field is either present and valid, or the row is rejected.
 */
@Component
public class SchemaValidator {

    /** The format users type/upload dates in. Storage stays ISO ({@code yyyy-MM-dd},
     * {@link LocalDate#toString()}'s own format) regardless of input format, so every
     * other consumer of a stored date (e.g. {@code FinanceSnapshotSyncService}) is
     * unaffected by this - only the accepted input format changes here. */
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Map<String, Object> validate(SubcategorySchema schema, Map<String, String> rawRow) throws FieldValidationException {
        Map<String, String> normalized = new LinkedHashMap<>();
        rawRow.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value));

        Map<String, Object> result = new LinkedHashMap<>();
        for (FieldDefinition field : schema.fields()) {
            String raw = normalized.get(field.name().toLowerCase(Locale.ROOT));
            if (raw == null || raw.isBlank()) {
                if (field.required()) {
                    throw new FieldValidationException("'" + field.label() + "' is required");
                }
                continue;
            }
            result.put(field.name(), coerce(field, raw.trim()));
        }
        return result;
    }

    private Object coerce(FieldDefinition field, String raw) throws FieldValidationException {
        return switch (field.type()) {
            case TEXT -> raw;
            case NUMBER -> parseNumber(field, raw);
            case DATE -> parseDate(field, raw);
            case ENUM -> parseEnum(field, raw);
        };
    }

    private BigDecimal parseNumber(FieldDefinition field, String raw) throws FieldValidationException {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new FieldValidationException("'" + field.label() + "' must be a number, got '" + raw + "'");
        }
    }

    private String parseDate(FieldDefinition field, String raw) throws FieldValidationException {
        try {
            return LocalDate.parse(raw, DISPLAY_DATE_FORMAT).toString();
        } catch (DateTimeParseException displayFormatFailed) {
            try {
                // Back-compat: anything already relying on the old ISO input format
                // (existing uploads, scripts) keeps working.
                return LocalDate.parse(raw).toString();
            } catch (DateTimeParseException isoAlsoFailed) {
                throw new FieldValidationException("'" + field.label() + "' must be a date (DD-MM-YYYY), got '" + raw + "'");
            }
        }
    }

    private String parseEnum(FieldDefinition field, String raw) throws FieldValidationException {
        String upper = raw.toUpperCase(Locale.ROOT).replace(' ', '_');
        boolean valid = field.options().stream().anyMatch(option -> option.equalsIgnoreCase(upper));
        if (!valid) {
            throw new FieldValidationException(
                    "'" + field.label() + "' must be one of " + field.options() + ", got '" + raw + "'");
        }
        return upper;
    }
}
