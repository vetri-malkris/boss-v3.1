package com.boss.forecasting.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinearRegressionForecasterTest {

    private final LinearRegressionForecaster forecaster = new LinearRegressionForecaster();

    @Test
    void projectsAPerfectLinearTrendExactly() {
        // 1000, 1100, 1200 -> next points continue +100/month exactly
        List<BigDecimal> history = List.of(new BigDecimal("1000"), new BigDecimal("1100"), new BigDecimal("1200"));

        List<BigDecimal> projected = forecaster.project(history, 3);

        assertThat(projected).hasSize(3);
        assertThat(projected.get(0)).isEqualByComparingTo("1300.00");
        assertThat(projected.get(1)).isEqualByComparingTo("1400.00");
        assertThat(projected.get(2)).isEqualByComparingTo("1500.00");
    }

    @Test
    void projectsAFlatSeriesAsFlat() {
        List<BigDecimal> history = List.of(new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("500"));

        List<BigDecimal> projected = forecaster.project(history, 2);

        assertThat(projected.get(0)).isEqualByComparingTo("500.00");
        assertThat(projected.get(1)).isEqualByComparingTo("500.00");
    }

    @Test
    void flooredAtZeroInsteadOfGoingNegative() {
        // Steeply decreasing -> would go negative within the horizon without flooring
        List<BigDecimal> history = List.of(new BigDecimal("300"), new BigDecimal("100"));

        List<BigDecimal> projected = forecaster.project(history, 5);

        assertThat(projected.get(4)).isEqualByComparingTo("0.00");
        assertThat(projected).allSatisfy(v -> assertThat(v.signum()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void requiresAtLeastTwoHistoricalPoints() {
        assertThatThrownBy(() -> forecaster.project(List.of(new BigDecimal("100")), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
