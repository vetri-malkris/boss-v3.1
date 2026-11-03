package com.boss.forecasting.dto;

/**
 * {@code available} is false when no starting cash balance is known at all (never
 * defaulted to zero). When available but cash doesn't run out within the requested
 * horizon, {@code monthsUntilExhausted}/{@code period} are null - not "never", just
 * "not within this horizon".
 */
public record RunwayForecastDto(boolean available, Integer monthsUntilExhausted, String period) {
}
