package com.boss.forecasting.dto;

/**
 * {@code hasEnoughData} is false (all other fields null) when fewer than 3 months of
 * snapshot history exist - the same minimum {@code TrendDetectionEngine} requires,
 * since a 2-point "trend" is just a difference (section 11).
 */
public record ForecastResponseDto(
        int horizonMonths,
        boolean hasEnoughData,
        MetricForecastDto revenue,
        MetricForecastDto expenses,
        MetricForecastDto cash,
        RunwayForecastDto runway,
        BreakEvenForecastDto breakEven
) {
    public static ForecastResponseDto insufficientData(int horizonMonths) {
        return new ForecastResponseDto(horizonMonths, false, null, null, null, null, null);
    }
}
