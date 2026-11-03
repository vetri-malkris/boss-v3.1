package com.boss.finance.kpi;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.calculator.MarginCalculator;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceKpiServiceTest {

    @Mock
    private SubcategoryDataRecordRepository repository;

    private FinanceKpiService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FinanceKpiService(repository, new MarginCalculator());
        // Default every subcategory this service queries to "no records" - each
        // test overrides only the ones it cares about.
        lenient().when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.POSITION))
                .thenReturn(List.of());
        lenient().when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PROFITABILITY))
                .thenReturn(List.of());
        lenient().when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.FORECAST))
                .thenReturn(List.of());
        lenient().when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.CAPITAL))
                .thenReturn(List.of());
    }

    @Test
    void everyFieldIsNullWhenNoFinanceRecordsExistAtAll() {
        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.cashPosition()).isNull();
        assertThat(result.workingCapital()).isNull();
        assertThat(result.currentRatio()).isNull();
        assertThat(result.grossProfit()).isNull();
        assertThat(result.grossMargin()).isNull();
        assertThat(result.operatingProfit()).isNull();
        assertThat(result.netProfit()).isNull();
        assertThat(result.netMargin()).isNull();
        assertThat(result.fundingRequirement()).isNull();
        assertThat(result.debtExposure()).isNull();
    }

    @Test
    void receivablesAndPayablesComeFromPositionsOwnBalanceSheetFields() {
        stubPosition(new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("150000"),
                new BigDecimal("2000000"), new BigDecimal("200000"), new BigDecimal("100000"), new BigDecimal("2650000"));

        assertThat(service.receivables(tenantId)).isEqualByComparingTo("300000");
        assertThat(service.payables(tenantId)).isEqualByComparingTo("200000");
    }

    @Test
    void receivablesAndPayablesAreNullWhenNoPositionRecordExists() {
        assertThat(service.receivables(tenantId)).isNull();
        assertThat(service.payables(tenantId)).isNull();
    }

    @Test
    void computesCashPositionWorkingCapitalAndCurrentRatioFromLatestPosition() {
        stubPosition(new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("150000"),
                new BigDecimal("2000000"), new BigDecimal("200000"), new BigDecimal("100000"), new BigDecimal("2650000"));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.cashPosition()).isEqualByComparingTo("500000");
        // current assets = 500000+300000+150000 = 950000; current liabilities = 200000+100000 = 300000
        assertThat(result.workingCapital()).isEqualByComparingTo("650000");
        assertThat(result.currentRatio()).isEqualByComparingTo("3.17");
    }

    @Test
    void currentRatioIsNullWhenCurrentLiabilitiesAreZero() {
        stubPosition(new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("150000"),
                new BigDecimal("2000000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2950000"));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.currentRatio()).isNull();
        assertThat(result.workingCapital()).isEqualByComparingTo("950000");
    }

    @Test
    void computesFullProfitabilityChainFromLatestProfitability() {
        stubProfitability(new BigDecimal("1000000"), new BigDecimal("600000"), new BigDecimal("250000"),
                new BigDecimal("20000"), new BigDecimal("30000"));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.grossProfit()).isEqualByComparingTo("400000");
        assertThat(result.grossMargin()).isEqualByComparingTo("40.00");
        assertThat(result.operatingProfit()).isEqualByComparingTo("150000");
        assertThat(result.netProfit()).isEqualByComparingTo("100000");
        assertThat(result.netMargin()).isEqualByComparingTo("10.00");
    }

    @Test
    void marginIsZeroNotNullWhenRevenueIsZeroButARecordExists() {
        stubProfitability(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO);

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.grossMargin()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netProfit()).isEqualByComparingTo("-5000");
    }

    @Test
    void fundingRequirementComesFromLatestForecastRecord() {
        SubcategoryDataRecord forecast = new SubcategoryDataRecord(Subcategory.FORECAST,
                Map.of("fundingRequirement", new BigDecimal("750000")), userId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.FORECAST))
                .thenReturn(List.of(forecast));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.fundingRequirement()).isEqualByComparingTo("750000");
    }

    @Test
    void handlesNumbersDeserializedAsIntegerOrDoubleNotJustBigDecimal() {
        // A jsonb round-trip through Hibernate/Jackson deserializes a generic
        // Map<String, Object>'s numbers as Integer/Long/Double, not BigDecimal -
        // this reproduces that exact shape rather than the idealized in-memory one.
        SubcategoryDataRecord position = new SubcategoryDataRecord(Subcategory.POSITION, Map.of(
                "cashAndBankBalance", 50000, "accountsReceivable", 30000, "inventoryValue", 20000,
                "fixedAssets", 500000, "accountsPayable", 90000.5, "otherLiabilities", 40000,
                "equity", 470000), userId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.POSITION))
                .thenReturn(List.of(position));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.cashPosition()).isEqualByComparingTo("50000");
        assertThat(result.workingCapital()).isEqualByComparingTo("-30000.5"); // (50000+30000+20000)-(90000.5+40000)
    }

    @Test
    void debtExposureSumsOutstandingDebtAcrossAllCapitalRecords() {
        SubcategoryDataRecord loan1 = new SubcategoryDataRecord(Subcategory.CAPITAL,
                Map.of("outstandingDebt", new BigDecimal("800000")), userId);
        SubcategoryDataRecord loan2 = new SubcategoryDataRecord(Subcategory.CAPITAL,
                Map.of("outstandingDebt", new BigDecimal("200000")), userId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.CAPITAL))
                .thenReturn(List.of(loan2, loan1));

        FinanceKpiDto result = service.summarize(tenantId);

        assertThat(result.debtExposure()).isEqualByComparingTo("1000000");
    }

    private void stubPosition(BigDecimal cash, BigDecimal ar, BigDecimal inventory, BigDecimal fixedAssets,
                               BigDecimal ap, BigDecimal otherLiabilities, BigDecimal equity) {
        SubcategoryDataRecord position = new SubcategoryDataRecord(Subcategory.POSITION, Map.of(
                "cashAndBankBalance", cash, "accountsReceivable", ar, "inventoryValue", inventory,
                "fixedAssets", fixedAssets, "accountsPayable", ap, "otherLiabilities", otherLiabilities,
                "equity", equity), userId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.POSITION))
                .thenReturn(List.of(position));
    }

    private void stubProfitability(BigDecimal revenue, BigDecimal cogs, BigDecimal opex, BigDecimal financeCost,
                                    BigDecimal tax) {
        SubcategoryDataRecord profitability = new SubcategoryDataRecord(Subcategory.PROFITABILITY, Map.of(
                "revenue", revenue, "cogs", cogs, "operatingExpenses", opex, "financeCost", financeCost,
                "tax", tax), userId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PROFITABILITY))
                .thenReturn(List.of(profitability));
    }
}
