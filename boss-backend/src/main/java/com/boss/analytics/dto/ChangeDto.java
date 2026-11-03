package com.boss.analytics.dto;

import java.math.BigDecimal;

public record ChangeDto(
        String metric,
        BigDecimal currentValue,
        BigDecimal previousValue,
        BigDecimal change,
        BigDecimal changePercentage,
        ChangeDirection direction
) {
}
