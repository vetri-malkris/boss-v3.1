package com.boss.financial.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MarginCalculatorTest {

    private final MarginCalculator calculator = new MarginCalculator();

    @Test
    void computesPercentageOfRevenue() {
        assertThat(calculator.calculate(new BigDecimal("250.00"), new BigDecimal("1000.00")))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void returnsZeroWhenRevenueIsZeroInsteadOfDividingByZero() {
        assertThat(calculator.calculate(new BigDecimal("-50.00"), BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }

    @Test
    void supportsNegativeMargin() {
        assertThat(calculator.calculate(new BigDecimal("-300.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo("-300.00");
    }
}
