package com.boss.workforce.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * Reads total people cost from the real People Cost subcategory form (HR
 * category) - gross salary + benefits + payroll tax, summed across every
 * department's record for the latest period that has one (handles multiple
 * departments: each is its own record for the same period). A record whose
 * {@code period} doesn't parse as "YYYY-MM" is skipped, never guessed at,
 * same convention {@code FinanceSnapshotSyncService} uses for Profitability.
 *
 * Deliberately informational only - never folded into Runway/Burn, because
 * Profitability's own Operating Expenses may already include payroll for a
 * tenant who entered it that way; adding this on top would double-count it.
 */
@Service
public class WorkforceService {

    private final SubcategoryDataRecordRepository repository;

    public WorkforceService(SubcategoryDataRecordRepository repository) {
        this.repository = repository;
    }

    public BigDecimal monthlyPeopleCost(UUID tenantId) {
        List<SubcategoryDataRecord> records = repository
                .findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST);

        YearMonth latestPeriod = records.stream()
                .map(this::parsePeriod)
                .filter(p -> p != null)
                .max(YearMonth::compareTo)
                .orElse(null);
        if (latestPeriod == null) {
            return null;
        }

        return records.stream()
                .filter(r -> latestPeriod.equals(parsePeriod(r)))
                .map(r -> number(r, "grossSalaryCost").add(number(r, "benefitsCost")).add(number(r, "payrollTaxCost")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private YearMonth parsePeriod(SubcategoryDataRecord record) {
        Object value = record.getFields().get("period");
        if (!(value instanceof String text)) {
            return null;
        }
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * A NUMBER field is a {@link BigDecimal} only immediately after
     * {@code SchemaValidator} coerces it in-memory - once persisted to the jsonb
     * column and read back, Hibernate/Jackson deserializes a generic
     * {@code Map<String, Object>}'s numbers as {@code Integer}/{@code Long}/
     * {@code Double}, not {@code BigDecimal}. Converting via {@code toString()}
     * handles every numeric subtype correctly regardless of which one came back.
     */
    private BigDecimal number(SubcategoryDataRecord record, String field) {
        Object value = record.getFields().get(field);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return BigDecimal.ZERO;
    }
}
