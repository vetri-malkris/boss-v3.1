package com.boss.risk.service;

import com.boss.analytics.dto.TrendDirection;
import com.boss.analytics.dto.TrendDto;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.risk.dto.EvidenceType;
import com.boss.risk.dto.RiskDto;
import com.boss.risk.dto.RiskSeverity;
import com.boss.risk.dto.WorkingCapitalDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {

    private final RiskEngine engine = new RiskEngine();

    @Test
    void flagsOperatingAtALossAsHighSeverity() {
        MonthlyFinancialSnapshot snapshot = snapshot("1000", "1500", null, null);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of());

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Operating at a loss");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
            assertThat(r.evidenceType()).isEqualTo(EvidenceType.FACT);
        });
    }

    @Test
    void flagsShortRunwayAsHighSeverity() {
        MonthlyFinancialSnapshot snapshot = snapshot("1000", "1000", new BigDecimal("10000"), new BigDecimal("3.0"));

        List<RiskDto> risks = engine.evaluate(snapshot, List.of());

        assertThat(risks).anySatisfy(r -> assertThat(r.risk()).isEqualTo("Short cash runway"));
    }

    @Test
    void doesNotFlagRunwayWhenSixMonthsOrMore() {
        MonthlyFinancialSnapshot snapshot = snapshot("2000", "1000", new BigDecimal("10000"), new BigDecimal("10.0"));

        List<RiskDto> risks = engine.evaluate(snapshot, List.of());

        assertThat(risks).noneMatch(r -> r.risk().equals("Short cash runway"));
    }

    @Test
    void flagsRevenueDeclineFromTrend() {
        MonthlyFinancialSnapshot snapshot = snapshot("1000", "500", null, null);
        TrendDto decliningRevenue = new TrendDto("revenue", TrendDirection.DECREASING, 4);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(decliningRevenue));

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Revenue decline");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH); // >= 4 months observed
            assertThat(r.possibleCause()).contains("not confirmed");
        });
    }

    @Test
    void healthyBusinessWithNoTrendsHasNoRisks() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", new BigDecimal("50000"), null);

        assertThat(engine.evaluate(snapshot, List.of())).isEmpty();
    }

    @Test
    void flagsReceivablesHighRelativeToRevenue() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(new BigDecimal("3000"), BigDecimal.ZERO, BigDecimal.ZERO);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital);

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Receivables high relative to revenue");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
        });
    }

    @Test
    void doesNotFlagReceivablesWhenWellBelowRevenueShare() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(engine.evaluate(snapshot, List.of(), workingCapital))
                .noneMatch(r -> r.risk().equals("Receivables high relative to revenue"));
    }

    @Test
    void flagsPayablesWhenExceedingCashBalance() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", new BigDecimal("2000"), new BigDecimal("10.0"));
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, new BigDecimal("3000"), BigDecimal.ZERO);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital);

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Payables may exceed available cash");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
        });
    }

    @Test
    void flagsOutstandingGstLiability() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1200"));

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital);

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("GST liability outstanding");
            assertThat(r.severity()).isEqualTo(RiskSeverity.MEDIUM);
        });
    }

    @Test
    void workingCapitalOverloadStillIncludesBaseRisks() {
        MonthlyFinancialSnapshot snapshot = snapshot("1000", "1500", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital);

        assertThat(risks).anySatisfy(r -> assertThat(r.risk()).isEqualTo("Operating at a loss"));
    }

    @Test
    void flagsLowCurrentRatioFromFinancialPosition() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        FinanceKpiDto financeKpis = financeKpis(new BigDecimal("0.8"), null);

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital, financeKpis);

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Low current ratio (Financial Position)");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
        });
    }

    @Test
    void doesNotFlagCurrentRatioWhenAtOrAboveOne() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        FinanceKpiDto financeKpis = financeKpis(new BigDecimal("1.5"), null);

        assertThat(engine.evaluate(snapshot, List.of(), workingCapital, financeKpis))
                .noneMatch(r -> r.risk().equals("Low current ratio (Financial Position)"));
    }

    @Test
    void doesNotFlagCurrentRatioWhenNull() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        FinanceKpiDto financeKpis = financeKpis(null, null);

        assertThat(engine.evaluate(snapshot, List.of(), workingCapital, financeKpis))
                .noneMatch(r -> r.risk().equals("Low current ratio (Financial Position)"));
    }

    @Test
    void flagsNetLossReportedFromProfitability() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        FinanceKpiDto financeKpis = financeKpis(null, new BigDecimal("-2000"));

        List<RiskDto> risks = engine.evaluate(snapshot, List.of(), workingCapital, financeKpis);

        assertThat(risks).anySatisfy(r -> {
            assertThat(r.risk()).isEqualTo("Net loss reported (Profitability)");
            assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
        });
    }

    @Test
    void doesNotFlagNetLossWhenProfitabilityIsPositiveOrAbsent() {
        MonthlyFinancialSnapshot snapshot = snapshot("10000", "5000", null, null);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        FinanceKpiDto financeKpis = financeKpis(null, new BigDecimal("2000"));

        assertThat(engine.evaluate(snapshot, List.of(), workingCapital, financeKpis))
                .noneMatch(r -> r.risk().equals("Net loss reported (Profitability)"));
    }

    private FinanceKpiDto financeKpis(BigDecimal currentRatio, BigDecimal netProfit) {
        return new FinanceKpiDto(null, null, currentRatio, null, null, null, netProfit, null, null, null);
    }

    private MonthlyFinancialSnapshot snapshot(String revenue, String expenses, BigDecimal cash, BigDecimal runway) {
        BigDecimal rev = new BigDecimal(revenue);
        BigDecimal exp = new BigDecimal(expenses);
        BigDecimal profit = rev.subtract(exp);
        BigDecimal margin = rev.signum() == 0 ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(rev, 2, java.math.RoundingMode.HALF_UP);
        MonthlyFinancialSnapshot snapshot = new MonthlyFinancialSnapshot("2026-01", rev, exp, profit, margin);
        if (cash != null) {
            snapshot.applyCashPosition(cash, runway);
        }
        return snapshot;
    }
}
