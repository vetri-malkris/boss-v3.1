package com.boss.forecasting.dto;

import java.math.BigDecimal;

public record ForecastPointDto(String period, BigDecimal value) {
}
