package com.boss.finance.gst.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads GST position from the real Tax-GST subcategory form (Compliance
 * category), not a separate ledger - picks the latest record whose
 * {@code filingPeriod} parses as a "YYYY-MM" period (a non-conforming entry
 * is skipped, never guessed at, same convention
 * {@code FinanceSnapshotSyncService} uses for Profitability's period field).
 */
@Service
public class GstService {

    private final SubcategoryDataRecordRepository repository;

    public GstService(SubcategoryDataRecordRepository repository) {
        this.repository = repository;
    }

    public BigDecimal outputGst(UUID tenantId) {
        return latestByPeriod(tenantId).map(r -> number(r, "outputGst")).orElse(null);
    }

    public BigDecimal inputGst(UUID tenantId) {
        return latestByPeriod(tenantId).map(r -> number(r, "inputGst")).orElse(null);
    }

    /** Output GST minus Input GST/ITC, floored at zero - a negative result is an
     * ITC credit position, not a liability. */
    public BigDecimal currentLiability(UUID tenantId) {
        return latestByPeriod(tenantId)
                .map(r -> number(r, "outputGst").subtract(number(r, "inputGst")).max(BigDecimal.ZERO))
                .orElse(null);
    }

    private Optional<SubcategoryDataRecord> latestByPeriod(UUID tenantId) {
        return repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST)
                .stream()
                .filter(r -> parsePeriod(r) != null)
                .max(Comparator.comparing(r -> parsePeriod(r)));
    }

    private YearMonth parsePeriod(SubcategoryDataRecord record) {
        Object value = record.getFields().get("filingPeriod");
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
