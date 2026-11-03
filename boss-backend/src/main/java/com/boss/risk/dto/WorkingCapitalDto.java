package com.boss.risk.dto;

import java.math.BigDecimal;

/** The three working-capital signals {@link com.boss.risk.service.RiskEngine}
 * checks alongside the monthly snapshot/trends - all real, current totals
 * from the Finance subcategory forms (Position's own accountsReceivable/
 * accountsPayable, Tax-GST's net liability), never estimated. Named
 * "Outstanding" rather than "Overdue"/"DueSoon": Position captures one
 * current balance with no due-date detail, so there's nothing to window by. */
public record WorkingCapitalDto(
        BigDecimal receivablesOutstanding,
        BigDecimal payablesOutstanding,
        BigDecimal gstLiability
) {
}
