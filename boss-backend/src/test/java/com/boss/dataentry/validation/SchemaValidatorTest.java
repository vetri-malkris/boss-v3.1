package com.boss.dataentry.validation;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.FieldDefinition;
import com.boss.dataentry.domain.SubcategorySchema;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator();

    private final SubcategorySchema hiringSchema = new SubcategorySchema(Subcategory.HIRING, List.of(
            FieldDefinition.text("positionTitle", "Position title", true),
            FieldDefinition.number("headcount", "Headcount", false),
            FieldDefinition.date("openedDate", "Opened date", true),
            FieldDefinition.enumField("status", "Status", true, "OPEN", "FILLED")));

    @Test
    void coercesEachFieldTypeAndDropsUnknownKeys() throws FieldValidationException {
        Map<String, Object> result = validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer",
                "headcount", "2",
                "openedDate", "2026-03-01",
                "status", "open",
                "someUnrelatedColumn", "ignored"));

        assertThat(result.get("positionTitle")).isEqualTo("Backend Engineer");
        assertThat(result.get("headcount")).isEqualTo(new BigDecimal("2"));
        assertThat(result.get("openedDate")).isEqualTo("2026-03-01");
        assertThat(result.get("status")).isEqualTo("OPEN");
        assertThat(result).doesNotContainKey("someUnrelatedColumn");
    }

    @Test
    void omitsAnAbsentOptionalField() throws FieldValidationException {
        Map<String, Object> result = validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer",
                "openedDate", "2026-03-01",
                "status", "OPEN"));

        assertThat(result).doesNotContainKey("headcount");
    }

    @Test
    void rejectsAMissingRequiredField() {
        assertThatThrownBy(() -> validator.validate(hiringSchema, Map.of("openedDate", "2026-03-01", "status", "OPEN")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Position title");
    }

    @Test
    void rejectsANonNumericValueForANumberField() {
        assertThatThrownBy(() -> validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer", "openedDate", "2026-03-01", "status", "OPEN", "headcount", "two")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Headcount");
    }

    @Test
    void acceptsTheDdMmYyyyDateFormatAndStoresItAsIso() throws FieldValidationException {
        Map<String, Object> result = validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer", "openedDate", "01-03-2026", "status", "OPEN"));

        assertThat(result.get("openedDate")).isEqualTo("2026-03-01");
    }

    @Test
    void stillAcceptsTheIsoDateFormatForBackwardCompatibility() throws FieldValidationException {
        Map<String, Object> result = validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer", "openedDate", "2026-03-01", "status", "OPEN"));

        assertThat(result.get("openedDate")).isEqualTo("2026-03-01");
    }

    @Test
    void rejectsAnUnparseableDate() {
        assertThatThrownBy(() -> validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer", "openedDate", "not-a-date", "status", "OPEN")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Opened date");
    }

    @Test
    void rejectsAnEnumValueOutsideTheAllowedOptions() {
        assertThatThrownBy(() -> validator.validate(hiringSchema, Map.of(
                "positionTitle", "Backend Engineer", "openedDate", "2026-03-01", "status", "CANCELLED")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Status");
    }
}
