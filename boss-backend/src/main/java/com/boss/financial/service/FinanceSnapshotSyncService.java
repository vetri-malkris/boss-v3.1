package com.boss.financial.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps {@link MonthlyFinancialSnapshot} (and therefore Overview's Business status,
 * trend charts, Monthly comparison, Forecast, and Runway) in sync with the Finance
 * Position/Profitability/People Cost forms - the single authoritative source for
 * those numbers now, replacing the old separate Transactions/Cash Position upload.
 * Called right after any POSITION, PROFITABILITY, or PEOPLE_COST
 * {@link SubcategoryDataRecord} is saved, regardless of whether that save came from
 * the single-record form, its bulk upload, or the category-wide upload - every
 * other subcategory is a no-op here.
 */
@Service
public class FinanceSnapshotSyncService {

    private final SnapshotService snapshotService;

    public FinanceSnapshotSyncService(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    public void sync(UUID userId, SubcategoryDataRecord record) {
        if (record.getSubcategory() == Subcategory.PROFITABILITY) {
            syncProfitability(userId, record);
        } else if (record.getSubcategory() == Subcategory.POSITION) {
            syncPosition(userId, record);
        } else if (record.getSubcategory() == Subcategory.PEOPLE_COST) {
            // People Cost is a third independent input to Runway's burn figure (see
            // SnapshotService.recomputeRunway) - saving it must refresh the
            // already-stored runway too, or it would silently go stale until the
            // next Position/Profitability edit.
            snapshotService.recomputeLatestRunway(record.getTenantId());
        }
    }

    private void syncProfitability(UUID userId, SubcategoryDataRecord record) {
        Map<String, Object> fields = record.getFields();
        Object periodValue = fields.get("period");
        if (!(periodValue instanceof String periodText) || periodText.isBlank()) {
            return;
        }

        String period;
        try {
            period = YearMonth.parse(periodText).toString();
        } catch (DateTimeParseException e) {
            // Period is free text on this form - a non-"YYYY-MM" entry (e.g. "Jan 2026")
            // simply can't be placed on the monthly snapshot timeline. The Profitability
            // record itself already saved successfully; this is only a missed projection,
            // not a reason to reject the user's form submission.
            return;
        }

        BigDecimal revenue = number(fields, "revenue");
        BigDecimal expenses = number(fields, "cogs")
                .add(number(fields, "operatingExpenses"))
                .add(number(fields, "financeCost"))
                .add(number(fields, "tax"));

        snapshotService.upsertPeriodFinancials(record.getTenantId(), userId, period, revenue, expenses);
    }

    private void syncPosition(UUID userId, SubcategoryDataRecord record) {
        Map<String, Object> fields = record.getFields();
        Object asOfDateValue = fields.get("asOfDate");
        if (!(asOfDateValue instanceof String asOfDateText)) {
            return;
        }

        String period;
        try {
            period = YearMonth.from(LocalDate.parse(asOfDateText)).toString();
        } catch (DateTimeParseException e) {
            return;
        }

        BigDecimal cashBalance = number(fields, "cashAndBankBalance");
        snapshotService.upsertCashPosition(record.getTenantId(), userId, period, cashBalance);
    }

    /**
     * A NUMBER field is a {@link BigDecimal} only immediately after
     * {@code SchemaValidator} coerces it in-memory - once persisted to the jsonb
     * column and read back, Hibernate/Jackson deserializes a generic
     * {@code Map<String, Object>}'s numbers as {@code Integer}/{@code Long}/
     * {@code Double}, not {@code BigDecimal}. Converting via {@code toString()}
     * handles every numeric subtype correctly regardless of which one came back.
     */
    private BigDecimal number(Map<String, Object> fields, String field) {
        Object value = fields.get(field);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return BigDecimal.ZERO;
    }
}
