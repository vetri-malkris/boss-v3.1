package com.boss.scenario.service;

import com.boss.financial.calculator.MarginCalculator;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.forecasting.calculator.ForecastProjectionDeriver;
import com.boss.forecasting.dto.ForecastPointDto;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.dto.MetricForecastDto;
import com.boss.forecasting.dto.RunwayForecastDto;
import com.boss.forecasting.service.ForecastService;
import com.boss.scenario.dto.ScenarioRequestDto;
import com.boss.scenario.dto.ScenarioResultDto;
import com.boss.scenario.dto.ScenarioSideDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies hypothetical revenue/expense/hiring changes to the tenant's existing
 * forecast trend and recalculates cash/runway/break-even from the adjusted
 * trajectory (section 12). The base forecast (from {@link ForecastService}) is only
 * ever read, never mutated - the "clone" in "clone -> apply variables -> recalculate"
 * is implicit: a new set of numbers is computed from it, and the original snapshots/
 * forecast are completely unaffected by running a scenario.
 */
@Service
public class ScenarioEngine {

    private final ForecastService forecastService;
    private final SnapshotService snapshotService;
    private final ForecastProjectionDeriver deriver;
    private final MarginCalculator marginCalculator;

    public ScenarioEngine(ForecastService forecastService, SnapshotService snapshotService,
                           ForecastProjectionDeriver deriver, MarginCalculator marginCalculator) {
        this.forecastService = forecastService;
        this.snapshotService = snapshotService;
        this.deriver = deriver;
        this.marginCalculator = marginCalculator;
    }

    public ScenarioResultDto simulate(UUID tenantId, ScenarioRequestDto request, int horizonMonths) {
        ForecastResponseDto forecast = forecastService.forecast(tenantId, horizonMonths);
        if (!forecast.hasEnoughData()) {
            return ScenarioResultDto.insufficientData(horizonMonths);
        }

        List<String> periods = forecast.revenue().points().stream().map(ForecastPointDto::period).toList();
        List<BigDecimal> baseRevenue = forecast.revenue().points().stream().map(ForecastPointDto::value).toList();
        List<BigDecimal> baseExpenses = forecast.expenses().points().stream().map(ForecastPointDto::value).toList();

        BigDecimal revenueFactor = BigDecimal.ONE.add(
                request.revenueChangePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal expenseFactor = BigDecimal.ONE.add(
                request.expenseChangePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal hiringCost = BigDecimal.valueOf(request.additionalHires()).multiply(request.avgMonthlyCostPerHire());

        List<BigDecimal> scenarioRevenue = new ArrayList<>(periods.size());
        List<BigDecimal> scenarioExpenses = new ArrayList<>(periods.size());
        for (int i = 0; i < periods.size(); i++) {
            scenarioRevenue.add(floorAtZero(baseRevenue.get(i).multiply(revenueFactor)));
            scenarioExpenses.add(floorAtZero(baseExpenses.get(i).multiply(expenseFactor).add(hiringCost)));
        }

        Optional<BigDecimal> startingCash = snapshotService.history(tenantId).stream()
                .reduce((first, second) -> second) // latest
                .map(MonthlyFinancialSnapshot::getCashBalance);

        ScenarioSideDto base = side(periods, baseRevenue, baseExpenses, startingCash);
        ScenarioSideDto scenario = side(periods, scenarioRevenue, scenarioExpenses, startingCash);

        return new ScenarioResultDto(horizonMonths, true, base, scenario);
    }

    private ScenarioSideDto side(List<String> periods, List<BigDecimal> revenue, List<BigDecimal> expenses,
                                  Optional<BigDecimal> startingCash) {
        BigDecimal nextRevenue = revenue.get(0);
        BigDecimal nextExpenses = expenses.get(0);
        BigDecimal nextProfit = nextRevenue.subtract(nextExpenses).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nextMargin = marginCalculator.calculate(nextProfit, nextRevenue);

        MetricForecastDto cash = startingCash.map(c -> deriver.projectCash(c, periods, revenue, expenses)).orElse(null);
        RunwayForecastDto runway = cash != null ? deriver.deriveRunway(cash) : new RunwayForecastDto(false, null, null);
        var breakEven = deriver.deriveBreakEven(nextProfit.signum() >= 0, periods.get(0), periods, revenue, expenses);

        return new ScenarioSideDto(nextRevenue, nextExpenses, nextProfit, nextMargin, cash, runway, breakEven);
    }

    private BigDecimal floorAtZero(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
