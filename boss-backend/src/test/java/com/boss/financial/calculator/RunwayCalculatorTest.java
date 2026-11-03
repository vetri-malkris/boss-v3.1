package com.boss.financial.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RunwayCalculatorTest {

    private final RunwayCalculator calculator = new RunwayCalculator();

    @Test
    void computesMonthsOfCashAtCurrentBurn() {
        assertThat(calculator.calculate(new BigDecimal("12000.00"), new BigDecimal("2000.00")))
                .isEqualByComparingTo("6.0");
    }

    @Test
    void isNullWhenNotBurningCash() {
        assertThat(calculator.calculate(new BigDecimal("12000.00"), new BigDecimal("-500.00"))).isNull();
        assertThat(calculator.calculate(new BigDecimal("12000.00"), BigDecimal.ZERO)).isNull();
    }

    @Test
    void isNullWhenNoCashBalanceSupplied() {
        assertThat(calculator.calculate(null, new BigDecimal("500.00"))).isNull();
    }
}
