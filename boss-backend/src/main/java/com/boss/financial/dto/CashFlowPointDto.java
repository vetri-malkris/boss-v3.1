package com.boss.financial.dto;

import java.math.BigDecimal;

/**
 * One month's recorded cash balance and its change from the prior month.
 * {@code netChange} is null whenever this month or the prior one has no recorded
 * cash balance - never backfilled from revenue/expenses, which are accrual figures
 * this system does not reconcile to cash (section: never fabricate certainty).
 */
public record CashFlowPointDto(String month, BigDecimal cashBalance, BigDecimal netChange) {
}
