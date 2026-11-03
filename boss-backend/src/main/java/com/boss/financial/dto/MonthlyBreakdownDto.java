package com.boss.financial.dto;

import java.math.BigDecimal;

/** One month's revenue/expenses/profit, used to render the trend chart. */
public record MonthlyBreakdownDto(String month, BigDecimal revenue, BigDecimal expenses, BigDecimal profit) {
}
