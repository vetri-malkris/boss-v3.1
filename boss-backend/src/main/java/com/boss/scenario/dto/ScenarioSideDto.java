package com.boss.scenario.dto;

import com.boss.forecasting.dto.BreakEvenForecastDto;
import com.boss.forecasting.dto.MetricForecastDto;
import com.boss.forecasting.dto.RunwayForecastDto;

import java.math.BigDecimal;

/** One side (base or scenario) of the comparison - next-month figures plus the full derived forecast. */
public record ScenarioSideDto(
        BigDecimal nextRevenue,
        BigDecimal nextExpenses,
        BigDecimal nextProfit,
        BigDecimal nextMargin,
        MetricForecastDto cash,
        RunwayForecastDto runway,
        BreakEvenForecastDto breakEven
) {
}
