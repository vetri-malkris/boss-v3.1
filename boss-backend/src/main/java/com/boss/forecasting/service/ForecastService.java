package com.boss.forecasting.service;

import com.boss.common.exception.BusinessValidationException;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.forecasting.calculator.ForecastProjectionDeriver;
import com.boss.forecasting.calculator.LinearRegressionForecaster;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.dto.MetricForecastDto;
import com.boss.forecasting.dto.RunwayForecastDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Composes {@link LinearRegressionForecaster} projections of revenue and expenses
 * into revenue/expense/cash/runway/break-even forecasts (section 11). Historical
 * snapshots in, a forecast result out - the AI orchestrator can use this as a tool
 * exactly like {@code DashboardService} once a question needs forward-looking data.
 */
@Service
public class ForecastService {

    public static final Set<Integer> SUPPORTED_HORIZONS = Set.of(3, 6, 12, 24, 36);
    static final int MIN_HISTORY_MONTHS = 3;
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SnapshotService snapshotService;
    private final LinearRegressionForecaster forecaster;
    private final ForecastProjectionDeriver deriver;

    public ForecastService(SnapshotService snapshotService, LinearRegressionForecaster forecaster,
                            ForecastProjectionDeriver deriver) {
        this.snapshotService = snapshotService;
        this.forecaster = forecaster;
        this.deriver = deriver;
    }

    public ForecastResponseDto forecast(UUID tenantId, int horizonMonths) {
        if (!SUPPORTED_HORIZONS.contains(horizonMonths)) {
            throw new BusinessValidationException("horizonMonths must be one of " + SUPPORTED_HORIZONS);
        }

        List<MonthlyFinancialSnapshot> history = snapshotService.history(tenantId);
        if (history.size() < MIN_HISTORY_MONTHS) {
            return ForecastResponseDto.insufficientData(horizonMonths);
        }

        List<BigDecimal> revenueHistory = history.stream().map(MonthlyFinancialSnapshot::getRevenue).toList();
        List<BigDecimal> expenseHistory = history.stream().map(MonthlyFinancialSnapshot::getExpenses).toList();
        List<BigDecimal> revenueProjected = forecaster.project(revenueHistory, horizonMonths);
        List<BigDecimal> expenseProjected = forecaster.project(expenseHistory, horizonMonths);

        YearMonth lastPeriod = YearMonth.parse(history.get(history.size() - 1).getPeriod());
        List<String> futurePeriods = new ArrayList<>(horizonMonths);
        for (int i = 1; i <= horizonMonths; i++) {
            futurePeriods.add(lastPeriod.plusMonths(i).format(PERIOD_FORMAT));
        }

        MetricForecastDto revenueForecast = deriver.toMetricForecast("revenue", futurePeriods, revenueProjected);
        MetricForecastDto expenseForecast = deriver.toMetricForecast("expenses", futurePeriods, expenseProjected);

        MonthlyFinancialSnapshot latest = history.get(history.size() - 1);
        MetricForecastDto cashForecast = null;
        RunwayForecastDto runwayForecast = new RunwayForecastDto(false, null, null);
        if (latest.getCashBalance() != null) {
            cashForecast = deriver.projectCash(latest.getCashBalance(), futurePeriods, revenueProjected, expenseProjected);
            runwayForecast = deriver.deriveRunway(cashForecast);
        }

        var breakEven = deriver.deriveBreakEven(latest.getProfit().signum() >= 0, latest.getPeriod(), futurePeriods,
                revenueProjected, expenseProjected);

        return new ForecastResponseDto(horizonMonths, true, revenueForecast, expenseForecast, cashForecast,
                runwayForecast, breakEven);
    }
}
