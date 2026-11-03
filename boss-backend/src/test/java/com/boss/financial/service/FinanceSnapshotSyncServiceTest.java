package com.boss.financial.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FinanceSnapshotSyncServiceTest {

    @Mock
    private SnapshotService snapshotService;

    private FinanceSnapshotSyncService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FinanceSnapshotSyncService(snapshotService);
    }

    @Test
    void profitabilityRecordUpsertsPeriodFinancialsWithExpensesSummedAcrossAllFourCostFields() {
        SubcategoryDataRecord record = record(Subcategory.PROFITABILITY, Map.of(
                "period", "2026-03", "revenue", new BigDecimal("1000"), "cogs", new BigDecimal("300"),
                "operatingExpenses", new BigDecimal("150"), "financeCost", new BigDecimal("20"),
                "tax", new BigDecimal("30")));

        service.sync(userId, record);

        verify(snapshotService).upsertPeriodFinancials(tenantId, userId, "2026-03",
                new BigDecimal("1000"), new BigDecimal("500"));
    }

    @Test
    void profitabilityFieldsComeBackAsIntegerLongOrDoubleAfterTheJsonbRoundTripNotJustBigDecimal() {
        SubcategoryDataRecord record = record(Subcategory.PROFITABILITY, Map.of(
                "period", "2026-03", "revenue", 1000, "cogs", 300L, "operatingExpenses", 150.0,
                "financeCost", 20, "tax", 30));

        service.sync(userId, record);

        ArgumentCaptor<BigDecimal> revenue = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> expenses = ArgumentCaptor.forClass(BigDecimal.class);
        verify(snapshotService).upsertPeriodFinancials(eq(tenantId), eq(userId), eq("2026-03"),
                revenue.capture(), expenses.capture());
        assertThat(revenue.getValue()).isEqualByComparingTo("1000");
        assertThat(expenses.getValue()).isEqualByComparingTo("500");
    }

    @Test
    void profitabilityWithANonYearMonthPeriodIsSkippedWithoutThrowing() {
        SubcategoryDataRecord record = record(Subcategory.PROFITABILITY, Map.of(
                "period", "Q1 2026", "revenue", new BigDecimal("1000"), "cogs", new BigDecimal("300"),
                "operatingExpenses", new BigDecimal("150"), "financeCost", new BigDecimal("20"),
                "tax", new BigDecimal("30")));

        service.sync(userId, record);

        verifyNoInteractions(snapshotService);
    }

    @Test
    void positionRecordUpsertsCashPositionWithThePeriodDerivedFromAsOfDate() {
        SubcategoryDataRecord record = record(Subcategory.POSITION, Map.of(
                "asOfDate", "2026-03-15", "cashAndBankBalance", new BigDecimal("500000")));

        service.sync(userId, record);

        verify(snapshotService).upsertCashPosition(tenantId, userId, "2026-03", new BigDecimal("500000"));
    }

    @Test
    void peopleCostRecordRecomputesTheLatestRunway() {
        SubcategoryDataRecord record = record(Subcategory.PEOPLE_COST, Map.of(
                "period", "2026-04", "department", "Engineering", "grossSalaryCost", new BigDecimal("100000")));

        service.sync(userId, record);

        verify(snapshotService).recomputeLatestRunway(tenantId);
    }

    @Test
    void otherSubcategoriesAreANoOp() {
        SubcategoryDataRecord record = record(Subcategory.HIRING, Map.of("positionTitle", "Engineer"));

        service.sync(userId, record);

        verifyNoInteractions(snapshotService);
    }

    private SubcategoryDataRecord record(Subcategory subcategory, Map<String, Object> fields) {
        SubcategoryDataRecord record = new SubcategoryDataRecord(subcategory, fields, userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        return record;
    }
}
