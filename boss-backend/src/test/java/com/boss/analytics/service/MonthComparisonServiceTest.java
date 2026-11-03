package com.boss.analytics.service;

import com.boss.analytics.dto.ChangeDirection;
import com.boss.analytics.dto.ChangeDto;
import com.boss.analytics.dto.MonthlyComparisonDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MonthComparisonServiceTest {

    private final MonthComparisonService service = new MonthComparisonService();

    @Test
    void isEmptyWithFewerThanTwoMonths() {
        assertThat(service.compare(List.of(snapshot("2026-01", "1000", "500")))).isEmpty();
        assertThat(service.compare(List.of())).isEmpty();
    }

    @Test
    void comparesTheLastTwoMonthsInHistoryOrder() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "1000", "500"), snapshot("2026-02", "1200", "400"));

        Optional<MonthlyComparisonDto> result = service.compare(history);

        assertThat(result).isPresent();
        MonthlyComparisonDto comparison = result.get();
        assertThat(comparison.currentPeriod()).isEqualTo("2026-02");
        assertThat(comparison.previousPeriod()).isEqualTo("2026-01");

        ChangeDto revenueChange = find(comparison, "revenue");
        assertThat(revenueChange.change()).isEqualByComparingTo("200.00");
        assertThat(revenueChange.changePercentage()).isEqualByComparingTo("20.00");
        assertThat(revenueChange.direction()).isEqualTo(ChangeDirection.INCREASE);

        ChangeDto expenseChange = find(comparison, "expenses");
        assertThat(expenseChange.change()).isEqualByComparingTo("-100.00");
        assertThat(expenseChange.direction()).isEqualTo(ChangeDirection.DECREASE);
    }

    @Test
    void treatsAZeroPreviousValueAsZeroPercentChangeInsteadOfDividingByZero() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "0", "0"), snapshot("2026-02", "500", "0"));

        ChangeDto revenueChange = find(service.compare(history).orElseThrow(), "revenue");

        assertThat(revenueChange.changePercentage()).isEqualByComparingTo("0.00");
    }

    private ChangeDto find(MonthlyComparisonDto comparison, String metric) {
        return comparison.changes().stream().filter(c -> c.metric().equals(metric)).findFirst().orElseThrow();
    }

    private MonthlyFinancialSnapshot snapshot(String period, String revenue, String expenses) {
        BigDecimal rev = new BigDecimal(revenue);
        BigDecimal exp = new BigDecimal(expenses);
        return new MonthlyFinancialSnapshot(period, rev, exp, rev.subtract(exp), BigDecimal.ZERO);
    }
}
