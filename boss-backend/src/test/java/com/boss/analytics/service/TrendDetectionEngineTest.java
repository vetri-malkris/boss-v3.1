package com.boss.analytics.service;

import com.boss.analytics.dto.TrendDirection;
import com.boss.analytics.dto.TrendDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendDetectionEngineTest {

    private final TrendDetectionEngine engine = new TrendDetectionEngine();

    @Test
    void reportsNoTrendsWithFewerThanThreeMonths() {
        List<MonthlyFinancialSnapshot> history = List.of(snapshot("2026-01", "1000"), snapshot("2026-02", "1200"));

        assertThat(engine.detectTrends(history)).isEmpty();
    }

    @Test
    void detectsIncreasingRevenue() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "1000"), snapshot("2026-02", "1200"), snapshot("2026-03", "1500"));

        TrendDto revenueTrend = find(engine.detectTrends(history), "revenue");

        assertThat(revenueTrend.direction()).isEqualTo(TrendDirection.INCREASING);
        assertThat(revenueTrend.monthsObserved()).isEqualTo(3);
    }

    @Test
    void detectsDecreasingRevenue() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "2000"), snapshot("2026-02", "1500"), snapshot("2026-03", "1000"));

        assertThat(find(engine.detectTrends(history), "revenue").direction()).isEqualTo(TrendDirection.DECREASING);
    }

    @Test
    void detectsStableRevenue() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "1000"), snapshot("2026-02", "1000"), snapshot("2026-03", "1000"));

        assertThat(find(engine.detectTrends(history), "revenue").direction()).isEqualTo(TrendDirection.STABLE);
    }

    @Test
    void detectsVolatileRevenueWhenDirectionChanges() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "1000"), snapshot("2026-02", "1500"), snapshot("2026-03", "900"));

        assertThat(find(engine.detectTrends(history), "revenue").direction()).isEqualTo(TrendDirection.VOLATILE);
    }

    @Test
    void onlyUsesTheMostRecentSixMonths() {
        List<MonthlyFinancialSnapshot> history = List.of(
                snapshot("2026-01", "5000"), // outside the 6-month window, should be ignored
                snapshot("2026-02", "1000"), snapshot("2026-03", "1200"), snapshot("2026-04", "1400"),
                snapshot("2026-05", "1600"), snapshot("2026-06", "1800"), snapshot("2026-07", "2000"));

        TrendDto revenueTrend = find(engine.detectTrends(history), "revenue");

        assertThat(revenueTrend.direction()).isEqualTo(TrendDirection.INCREASING);
        assertThat(revenueTrend.monthsObserved()).isEqualTo(6);
    }

    private TrendDto find(List<TrendDto> trends, String metric) {
        return trends.stream().filter(t -> t.metric().equals(metric)).findFirst().orElseThrow();
    }

    private MonthlyFinancialSnapshot snapshot(String period, String revenue) {
        return new MonthlyFinancialSnapshot(period, new BigDecimal(revenue), BigDecimal.ZERO, new BigDecimal(revenue),
                BigDecimal.ZERO);
    }
}
