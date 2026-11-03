package com.boss.financial.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * runway (months) = cash balance / monthly burn. Returns {@code null} - meaning
 * "not applicable" - when there is no burn (cash-flow positive/breakeven) or no
 * cash balance was supplied, rather than fabricating a number or dividing by zero.
 */
@Component
public class RunwayCalculator {

    public BigDecimal calculate(BigDecimal cashBalance, BigDecimal monthlyBurn) {
        if (cashBalance == null || monthlyBurn == null || monthlyBurn.signum() <= 0) {
            return null;
        }
        return cashBalance.divide(monthlyBurn, 1, RoundingMode.HALF_UP);
    }
}
