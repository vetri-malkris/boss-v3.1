package com.boss.finance.gst.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GstServiceTest {

    @Mock
    private SubcategoryDataRecordRepository repository;

    private GstService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GstService(repository);
    }

    @Test
    void currentLiabilityIsOutputMinusInputGstForTheLatestPeriod() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of(record("2026-05", "132000", "85000")));

        assertThat(service.currentLiability(tenantId)).isEqualByComparingTo("47000");
        assertThat(service.outputGst(tenantId)).isEqualByComparingTo("132000");
        assertThat(service.inputGst(tenantId)).isEqualByComparingTo("85000");
    }

    @Test
    void picksTheLatestParsablePeriodAcrossMultipleRecords() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of(record("2026-04", "120000", "80000"), record("2026-06", "60000", "10000")));

        assertThat(service.currentLiability(tenantId)).isEqualByComparingTo("50000");
    }

    @Test
    void negativeNetLiabilityIsFlooredAtZeroBecauseItsACreditNotADebt() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of(record("2026-04", "50000", "80000")));

        assertThat(service.currentLiability(tenantId)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void unparsablePeriodIsSkippedNotGuessedAt() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of(record("Q1 2026", "120000", "80000")));

        assertThat(service.currentLiability(tenantId)).isNull();
        assertThat(service.outputGst(tenantId)).isNull();
    }

    @Test
    void noRecordsMeansNull() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of());

        assertThat(service.currentLiability(tenantId)).isNull();
    }

    @Test
    void fieldsComeBackAsIntegerLongOrDoubleAfterTheJsonbRoundTripNotJustBigDecimal() {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.TAX_GST,
                Map.of("filingPeriod", "2026-05", "outputGst", 132000, "inputGst", 85000L), userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.TAX_GST))
                .thenReturn(List.of(record));

        assertThat(service.currentLiability(tenantId)).isEqualByComparingTo("47000");
    }

    private SubcategoryDataRecord record(String filingPeriod, String outputGst, String inputGst) {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.TAX_GST, Map.of(
                "filingPeriod", filingPeriod, "outputGst", new BigDecimal(outputGst), "inputGst", new BigDecimal(inputGst)),
                userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        return record;
    }
}
