package com.boss.forecasting.dto;

import java.util.List;

public record MetricForecastDto(String metric, List<ForecastPointDto> points) {
}
