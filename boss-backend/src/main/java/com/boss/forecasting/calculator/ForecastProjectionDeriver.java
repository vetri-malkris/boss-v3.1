package com.boss.forecasting.calculator;

import com.boss.forecasting.dto.BreakEvenForecastDto;
import com.boss.forecasting.dto.ForecastPointDto;
import com.boss.forecasting.dto.MetricForecastDto;
import com.boss.forecasting.dto.RunwayForecastDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a pair of projected revenue/expense series into the derived cash, runway and
 * break-even views. Shared by {@code ForecastService} (projects the actual trend) and
 * {@code ScenarioEngine} (projects the trend with hypothetical adjustments applied) so
 * both stay consistent without duplicating this arithmetic.
 */
@Component
public class ForecastProjectionDeriver {

    public MetricForecastDto toMetricForecast(String metric, List<String> periods, List<BigDecimal> values) {
        List<ForecastPointDto> points = new ArrayList<>(periods.size());
        for (int i = 0; i < periods.size(); i++) {
            points.add(new ForecastPointDto(periods.get(i), values.get(i)));
        }
        return new MetricForecastDto(metric, points);
    }

    public MetricForecastDto projectCash(BigDecimal startingCash, List<String> periods, List<BigDecimal> revenue,
                                          List<BigDecimal> expenses) {
        List<ForecastPointDto> points = new ArrayList<>(periods.size());
        BigDecimal running = startingCash;
        for (int i = 0; i < periods.size(); i++) {
            running = running.add(revenue.get(i)).subtract(expenses.get(i));
            points.add(new ForecastPointDto(periods.get(i), running));
        }
        return new MetricForecastDto("cash", points);
    }

    public RunwayForecastDto deriveRunway(MetricForecastDto cashForecast) {
        for (int i = 0; i < cashForecast.points().size(); i++) {
            ForecastPointDto point = cashForecast.points().get(i);
            if (point.value().signum() <= 0) {
                return new RunwayForecastDto(true, i + 1, point.period());
            }
        }
        return new RunwayForecastDto(true, null, null);
    }

    /**
     * @param alreadyBreakEven whether the reference point (the latest actual month for a
     *                         real forecast, or the first scenario month for a scenario)
     *                         is already at or above zero profit
     * @param referencePeriod  the period to report when {@code alreadyBreakEven} is true
     */
    public BreakEvenForecastDto deriveBreakEven(boolean alreadyBreakEven, String referencePeriod, List<String> periods,
                                                 List<BigDecimal> revenue, List<BigDecimal> expenses) {
        if (alreadyBreakEven) {
            return new BreakEvenForecastDto(true, 0, referencePeriod);
        }
        for (int i = 0; i < periods.size(); i++) {
            if (revenue.get(i).compareTo(expenses.get(i)) >= 0) {
                return new BreakEvenForecastDto(false, i + 1, periods.get(i));
            }
        }
        return new BreakEvenForecastDto(false, null, null);
    }
}
