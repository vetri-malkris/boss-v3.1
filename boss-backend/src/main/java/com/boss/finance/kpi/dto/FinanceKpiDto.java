package com.boss.finance.kpi.dto;

import java.math.BigDecimal;

/**
 * KPIs derived from the manually-entered Finance subcategory forms (Position,
 * Profitability, Budget & Forecast, Capital) - a distinct authoritative source
 * from the transaction-upload-driven {@code MetricsDto} revenue/expenses/profit,
 * never merged into it. Every field is {@code null} when its source subcategory
 * has no record at all yet (never a fabricated zero).
 */
public record FinanceKpiDto(
        BigDecimal cashPosition,
        BigDecimal workingCapital,
        BigDecimal currentRatio,
        BigDecimal grossProfit,
        BigDecimal grossMargin,
        BigDecimal operatingProfit,
        BigDecimal netProfit,
        BigDecimal netMargin,
        BigDecimal fundingRequirement,
        BigDecimal debtExposure
) {
}
