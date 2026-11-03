package com.boss.forecasting.dto;

/**
 * {@code monthsFromNow} and {@code period} are both null when the break-even point
 * isn't reached within the requested horizon - never guessed beyond the data.
 */
public record BreakEvenForecastDto(boolean alreadyBreakEven, Integer monthsFromNow, String period) {
}
