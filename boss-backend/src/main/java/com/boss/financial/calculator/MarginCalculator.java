package com.boss.financial.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** profitMargin = profit / revenue * 100. Zero (not a divide-by-zero error) when revenue is zero. */
@Component
public class MarginCalculator {

    public BigDecimal calculate(BigDecimal profit, BigDecimal revenue) {
        if (revenue.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);
    }
}
