package com.boss.scenario.service;

import com.boss.financial.calculator.MarginCalculator;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.forecasting.calculator.ForecastProjectionDeriver;
import com.boss.forecasting.calculator.LinearRegressionForecaster;
import com.boss.forecasting.service.ForecastService;
import com.boss.scenario.dto.ScenarioRequestDto;
import com.boss.scenario.dto.ScenarioResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Exercises the real forecasting math (ForecastService/ForecastProjectionDeriver/
 * LinearRegressionForecaster) end to end - only the snapshot repository boundary is
 * mocked - so these tests catch real arithmetic mistakes, not just wiring mistakes.
 */
@ExtendWith(MockitoExtension.class)
class ScenarioEngineTest {

    @Mock
    private SnapshotService snapshotService;

    private ScenarioEngine scenarioEngine;
    private final UUID tenantId = UUID.randomUUID();

    private static final ScenarioRequestDto NO_CHANGE =
            new ScenarioRequestDto(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);

    @BeforeEach
    void setUp() {
        ForecastProjectionDeriver deriver = new ForecastProjectionDeriver();
        ForecastService forecastService = new ForecastService(snapshotService, new LinearRegressionForecaster(), deriver);
        scenarioEngine = new ScenarioEngine(forecastService, snapshotService, deriver, new MarginCalculator());
    }

    @Test
    void reportsInsufficientDataWithFewerThanThreeMonths() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(snapshot("2026-01", "1000", "800", null)));

        ScenarioResultDto result = scenarioEngine.simulate(tenantId, NO_CHANGE, 6);

        assertThat(result.hasEnoughData()).isFalse();
        assertThat(result.base()).isNull();
    }

    @Test
    void noChangeScenarioMatchesBaseExactly() {
        stubThreeFlatMonths();

        ScenarioResultDto result = scenarioEngine.simulate(tenantId, NO_CHANGE, 3);

        assertThat(result.scenario().nextRevenue()).isEqualByComparingTo(result.base().nextRevenue());
        assertThat(result.scenario().nextExpenses()).isEqualByComparingTo(result.base().nextExpenses());
        assertThat(result.scenario().nextProfit()).isEqualByComparingTo(result.base().nextProfit());
    }

    @Test
    void revenueDropPercentReducesScenarioRevenueProportionally() {
        stubThreeFlatMonths(); // base next-month revenue is 1000

        ScenarioRequestDto request = new ScenarioRequestDto(new BigDecimal("-10"), BigDecimal.ZERO, 0, BigDecimal.ZERO);
        ScenarioResultDto result = scenarioEngine.simulate(tenantId, request, 3);

        assertThat(result.base().nextRevenue()).isEqualByComparingTo("1000.00");
        assertThat(result.scenario().nextRevenue()).isEqualByComparingTo("900.00");
    }

    @Test
    void hiringAddsItsFullCostToScenarioExpenses() {
        stubThreeFlatMonths(); // base next-month expenses is 800

        ScenarioRequestDto request = new ScenarioRequestDto(BigDecimal.ZERO, BigDecimal.ZERO, 2, new BigDecimal("1000"));
        ScenarioResultDto result = scenarioEngine.simulate(tenantId, request, 3);

        assertThat(result.base().nextExpenses()).isEqualByComparingTo("800.00");
        assertThat(result.scenario().nextExpenses()).isEqualByComparingTo("2800.00"); // 800 + 2*1000
    }

    @Test
    void runwayIsUnavailableOnBothSidesWithoutAKnownCashBalance() {
        stubThreeFlatMonths();

        ScenarioResultDto result = scenarioEngine.simulate(tenantId, NO_CHANGE, 3);

        assertThat(result.base().runway().available()).isFalse();
        assertThat(result.scenario().runway().available()).isFalse();
    }

    @Test
    void increasingExpensesShortensScenarioRunwayComparedToBase() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "1000", "900", new BigDecimal("2000")),
                snapshot("2026-02", "1000", "900", new BigDecimal("2000")),
                snapshot("2026-03", "1000", "900", new BigDecimal("2000"))));

        ScenarioRequestDto request = new ScenarioRequestDto(BigDecimal.ZERO, new BigDecimal("50"), 0, BigDecimal.ZERO);
        ScenarioResultDto result = scenarioEngine.simulate(tenantId, request, 12);

        assertThat(result.base().runway().available()).isTrue();
        assertThat(result.scenario().runway().available()).isTrue();
        // Base: profit +100/mo on 2000 cash -> never runs out (null). Scenario: expenses
        // +50% (900 -> 1350) -> losing 350/mo -> runs out within the horizon.
        assertThat(result.base().runway().monthsUntilExhausted()).isNull();
        assertThat(result.scenario().runway().monthsUntilExhausted()).isNotNull();
    }

    private void stubThreeFlatMonths() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "1000", "800", null),
                snapshot("2026-02", "1000", "800", null),
                snapshot("2026-03", "1000", "800", null)));
    }

    private MonthlyFinancialSnapshot snapshot(String period, String revenue, String expenses, BigDecimal cash) {
        BigDecimal rev = new BigDecimal(revenue);
        BigDecimal exp = new BigDecimal(expenses);
        MonthlyFinancialSnapshot snapshot = new MonthlyFinancialSnapshot(period, rev, exp, rev.subtract(exp), BigDecimal.ZERO);
        if (cash != null) {
            snapshot.applyCashPosition(cash, null);
        }
        return snapshot;
    }
}
