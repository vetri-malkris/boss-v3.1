package com.boss.dashboard.dto;

import java.math.BigDecimal;

/** {@code netCashFlow} is the latest month's change in recorded cash balance from
 * the month before it - null when either month has no recorded balance, never
 * derived from revenue/expenses (see {@code CashFlowCalculator}). */
public record MetricsDto(
        String period,
        BigDecimal revenue,
        BigDecimal expenses,
        BigDecimal profit,
        BigDecimal margin,
        BigDecimal cashBalance,
        BigDecimal runwayMonths,
        BigDecimal receivables,
        BigDecimal payables,
        BigDecimal netCashFlow,
        BigDecimal gstLiability,
        BigDecimal outputGst,
        BigDecimal inputGst,
        BigDecimal monthlyPeopleCost
) {
}
