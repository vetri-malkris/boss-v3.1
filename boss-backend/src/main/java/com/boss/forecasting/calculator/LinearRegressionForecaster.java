package com.boss.forecasting.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic ordinary-least-squares linear regression over a metric's monthly
 * history, extrapolated forward. This is the entire "statistical forecasting"
 * engine for now (section 11) - a real ML model can replace it later without
 * changing {@code ForecastService}'s public contract, since both just implement
 * "history in, N future points out".
 *
 * <p>Values are floored at zero - a forecast of negative revenue or expenses has
 * no business meaning, so the projection is clamped rather than reported as-is.
 */
@Component
public class LinearRegressionForecaster {

    public List<BigDecimal> project(List<BigDecimal> history, int horizonMonths) {
        if (history.size() < 2) {
            throw new IllegalArgumentException("At least 2 historical points are required to fit a trend");
        }

        int n = history.size();
        double xMean = (n - 1) / 2.0;
        double yMean = history.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);

        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            double x = i - xMean;
            double y = history.get(i).doubleValue() - yMean;
            numerator += x * y;
            denominator += x * x;
        }
        double slope = denominator == 0 ? 0 : numerator / denominator;
        double intercept = yMean - slope * xMean;

        List<BigDecimal> projected = new ArrayList<>(horizonMonths);
        for (int step = 0; step < horizonMonths; step++) {
            int x = n + step;
            double y = intercept + slope * x;
            BigDecimal value = BigDecimal.valueOf(Math.max(0, y)).round(new MathContext(10)).setScale(2, RoundingMode.HALF_UP);
            projected.add(value);
        }
        return projected;
    }
}
