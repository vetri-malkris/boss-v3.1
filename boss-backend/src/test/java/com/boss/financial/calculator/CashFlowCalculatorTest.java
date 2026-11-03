package com.boss.financial.calculator;

import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.dto.CashFlowPointDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashFlowCalculatorTest {

    private final CashFlowCalculator calculator = new CashFlowCalculator();

    @Test
    void computesNetChangeBetweenConsecutiveMonthsWithRecordedBalances() {
        MonthlyFinancialSnapshot jan = snapshot("2026-01", "1000.00");
        MonthlyFinancialSnapshot feb = snapshot("2026-02", "1500.00");
        MonthlyFinancialSnapshot mar = snapshot("2026-03", "1300.00");

        List<CashFlowPointDto> result = calculator.calculate(List.of(jan, feb, mar));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).netChange()).isNull();
        assertThat(result.get(1).netChange()).isEqualByComparingTo("500.00");
        assertThat(result.get(2).netChange()).isEqualByComparingTo("-200.00");
    }

    @Test
    void netChangeIsNullWhenEitherMonthHasNoRecordedBalanceRatherThanBeingBackfilledFromRevenueOrExpenses() {
        MonthlyFinancialSnapshot jan = snapshot("2026-01", "1000.00");
        MonthlyFinancialSnapshot feb = snapshotWithoutCash("2026-02");
        MonthlyFinancialSnapshot mar = snapshot("2026-03", "1300.00");

        List<CashFlowPointDto> result = calculator.calculate(List.of(jan, feb, mar));

        assertThat(result.get(0).netChange()).isNull();
        assertThat(result.get(1).cashBalance()).isNull();
        assertThat(result.get(1).netChange()).isNull();
        assertThat(result.get(2).netChange()).isNull();
    }

    @Test
    void returnsEmptyListForNoHistory() {
        assertThat(calculator.calculate(List.of())).isEmpty();
    }

    private static MonthlyFinancialSnapshot snapshot(String period, String cashBalance) {
        MonthlyFinancialSnapshot snapshot = new MonthlyFinancialSnapshot(period, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        snapshot.applyCashPosition(new BigDecimal(cashBalance), null);
        return snapshot;
    }

    private static MonthlyFinancialSnapshot snapshotWithoutCash(String period) {
        return new MonthlyFinancialSnapshot(period, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
