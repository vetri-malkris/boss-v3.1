package com.boss.analytics.service;

import com.boss.analytics.dto.ChangeDirection;
import com.boss.analytics.dto.ChangeDto;
import com.boss.analytics.dto.MonthlyComparisonDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic current-vs-previous-month comparison (section 7). The LLM never
 * computes this - it only ever reads the result as evidence.
 */
@Service
public class MonthComparisonService {

    public Optional<MonthlyComparisonDto> compare(List<MonthlyFinancialSnapshot> historyAscending) {
        if (historyAscending.size() < 2) {
            return Optional.empty();
        }

        MonthlyFinancialSnapshot current = historyAscending.get(historyAscending.size() - 1);
        MonthlyFinancialSnapshot previous = historyAscending.get(historyAscending.size() - 2);

        List<ChangeDto> changes = List.of(
                change("revenue", current.getRevenue(), previous.getRevenue()),
                change("expenses", current.getExpenses(), previous.getExpenses()),
                change("profit", current.getProfit(), previous.getProfit()),
                change("margin", current.getMargin(), previous.getMargin()));

        return Optional.of(new MonthlyComparisonDto(current.getPeriod(), previous.getPeriod(), changes));
    }

    private ChangeDto change(String metric, BigDecimal currentValue, BigDecimal previousValue) {
        BigDecimal delta = currentValue.subtract(previousValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercentage = previousValue.signum() == 0
                ? BigDecimal.ZERO
                : delta.multiply(BigDecimal.valueOf(100)).divide(previousValue.abs(), 2, RoundingMode.HALF_UP);
        ChangeDirection direction = delta.signum() > 0 ? ChangeDirection.INCREASE
                : delta.signum() < 0 ? ChangeDirection.DECREASE
                : ChangeDirection.STABLE;
        return new ChangeDto(metric, currentValue, previousValue, delta, changePercentage, direction);
    }
}
