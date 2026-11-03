package com.boss.dataentry.schema;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.FieldDefinition;
import com.boss.dataentry.domain.FieldType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SubcategorySchemaRegistryTest {

    private final SubcategorySchemaRegistry registry = new SubcategorySchemaRegistry();

    @Test
    void everyDataEntrySubcategoryIsRegistered() {
        Set<Subcategory> expected = Set.of(
                Subcategory.WORKFORCE, Subcategory.PEOPLE_COST, Subcategory.PRODUCTIVITY,
                Subcategory.HIRING, Subcategory.ATTRITION, Subcategory.CAPABILITY,
                Subcategory.POSITION, Subcategory.LIQUIDITY, Subcategory.PROFITABILITY,
                Subcategory.FORECAST, Subcategory.COST, Subcategory.CAPITAL,
                Subcategory.REPORTING, Subcategory.WORKING_CAPITAL, Subcategory.ASSETS_LIABILITIES,
                Subcategory.RECONCILIATION, Subcategory.ACCOUNTING_CONTROLS,
                Subcategory.AUDIT_STATUS, Subcategory.FINDINGS, Subcategory.AUDIT_CONTROLS,
                Subcategory.AUDIT_EXCEPTIONS, Subcategory.AUDIT_ACTIONS,
                Subcategory.COMPLIANCE_STATUS, Subcategory.TAX_GST, Subcategory.REGULATORY,
                Subcategory.FILINGS, Subcategory.COMPLIANCE_EXCEPTIONS,
                Subcategory.RISK_EXPOSURE, Subcategory.RISK_FINANCIAL, Subcategory.RISK_OPERATIONAL,
                Subcategory.RISK_COMPLIANCE, Subcategory.RISK_TECHNOLOGY);

        Set<Subcategory> actual = registry.all().stream().map(schema -> schema.subcategory()).collect(Collectors.toSet());

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * Every Finance subcategory now also has a manual entry schema, shown alongside
     * (not instead of) its real transaction-derived panels - the same pattern Risk's
     * manual register already uses next to its real auto-detected risk panel.
     * Mitigation is the one subcategory that stays manual-entry-free: it's already the
     * real risk -> recommendation -> action -> outcome workflow, not a register.
     */
    @Test
    void onlyMitigationStaysWithoutAManualEntrySchema() {
        assertThat(registry.isDefined(Subcategory.POSITION)).isTrue();
        assertThat(registry.isDefined(Subcategory.LIQUIDITY)).isTrue();
        assertThat(registry.isDefined(Subcategory.PROFITABILITY)).isTrue();
        assertThat(registry.isDefined(Subcategory.FORECAST)).isTrue();
        assertThat(registry.isDefined(Subcategory.COST)).isTrue();
        assertThat(registry.isDefined(Subcategory.RISK_MITIGATION)).isFalse();
    }

    @Test
    void everySchemaHasUniqueFieldNamesAndNonEmptyOptionsForEveryEnumField() {
        for (var schema : registry.all()) {
            Set<String> names = new HashSet<>();
            for (FieldDefinition field : schema.fields()) {
                assertThat(names.add(field.name()))
                        .withFailMessage("Duplicate field name '%s' in %s", field.name(), schema.subcategory())
                        .isTrue();
                if (field.type() == FieldType.ENUM) {
                    assertThat(field.options())
                            .withFailMessage("ENUM field '%s' in %s has no options", field.name(), schema.subcategory())
                            .isNotEmpty();
                }
            }
        }
    }

    @Test
    void everySchemaHasAtLeastOneRequiredField() {
        for (var schema : registry.all()) {
            assertThat(schema.fields())
                    .withFailMessage("%s has no required fields", schema.subcategory())
                    .anyMatch(FieldDefinition::required);
        }
    }
}
