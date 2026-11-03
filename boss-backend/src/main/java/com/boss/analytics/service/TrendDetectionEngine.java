package com.boss.analytics.service;

import com.boss.analytics.dto.TrendDirection;
import com.boss.analytics.dto.TrendDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Deterministic trend classification over recent monthly snapshots (section 8).
 * A metric needs at least 3 months of history before a trend is reported at all -
 * two points is a difference, not a trend.
 */
@Service
public class TrendDetectionEngine {

    private static final int MAX_WINDOW = 6;
    private static final int MIN_POINTS = 3;

    public List<TrendDto> detectTrends(List<MonthlyFinancialSnapshot> historyAscending) {
        List<TrendDto> trends = new ArrayList<>();
        trends.addAll(detect("revenue", historyAscending, MonthlyFinancialSnapshot::getRevenue));
        trends.addAll(detect("expenses", historyAscending, MonthlyFinancialSnapshot::getExpenses));
        trends.addAll(detect("profit", historyAscending, MonthlyFinancialSnapshot::getProfit));
        trends.addAll(detect("margin", historyAscending, MonthlyFinancialSnapshot::getMargin));
        return trends;
    }

    private List<TrendDto> detect(String metric, List<MonthlyFinancialSnapshot> historyAscending,
                                   Function<MonthlyFinancialSnapshot, BigDecimal> extractor) {
        int size = historyAscending.size();
        if (size < MIN_POINTS) {
            return List.of();
        }

        List<MonthlyFinancialSnapshot> window = historyAscending.subList(Math.max(0, size - MAX_WINDOW), size);
        List<BigDecimal> values = window.stream().map(extractor).toList();

        boolean anyIncrease = false;
        boolean anyDecrease = false;
        for (int i = 1; i < values.size(); i++) {
            int cmp = values.get(i).compareTo(values.get(i - 1));
            if (cmp > 0) {
                anyIncrease = true;
            } else if (cmp < 0) {
                anyDecrease = true;
            }
        }

        TrendDirection direction;
        if (anyIncrease && anyDecrease) {
            direction = TrendDirection.VOLATILE;
        } else if (anyIncrease) {
            direction = TrendDirection.INCREASING;
        } else if (anyDecrease) {
            direction = TrendDirection.DECREASING;
        } else {
            direction = TrendDirection.STABLE;
        }

        return List.of(new TrendDto(metric, direction, values.size()));
    }
}
