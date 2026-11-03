package com.boss.forecasting.service;

import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.forecasting.calculator.ForecastProjectionDeriver;
import com.boss.forecasting.calculator.LinearRegressionForecaster;
import com.boss.forecasting.dto.ForecastResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private SnapshotService snapshotService;

    private ForecastService forecastService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        forecastService = new ForecastService(snapshotService, new LinearRegressionForecaster(), new ForecastProjectionDeriver());
    }

    @Test
    void reportsInsufficientDataWithFewerThanThreeMonths() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(snapshot("2026-01", "1000", "500", null)));

        ForecastResponseDto result = forecastService.forecast(tenantId, 12);

        assertThat(result.hasEnoughData()).isFalse();
        assertThat(result.revenue()).isNull();
    }

    @Test
    void rejectsAnUnsupportedHorizon() {
        assertThatThrownBy(() -> forecastService.forecast(tenantId, 7)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void projectsRevenueAndExpensesForwardFromHistory() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "1000", "800", null),
                snapshot("2026-02", "1100", "850", null),
                snapshot("2026-03", "1200", "900", null)));

        ForecastResponseDto result = forecastService.forecast(tenantId, 3);

        assertThat(result.hasEnoughData()).isTrue();
        assertThat(result.revenue().points()).hasSize(3);
        assertThat(result.revenue().points().get(0).period()).isEqualTo("2026-04");
        assertThat(result.revenue().points().get(0).value()).isEqualByComparingTo("1300.00");
    }

    @Test
    void alreadyBreakEvenWhenLatestProfitIsNonNegative() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "1000", "800", null),
                snapshot("2026-02", "1100", "850", null),
                snapshot("2026-03", "1200", "900", null)));

        ForecastResponseDto result = forecastService.forecast(tenantId, 3);

        assertThat(result.breakEven().alreadyBreakEven()).isTrue();
        assertThat(result.breakEven().monthsFromNow()).isZero();
    }

    @Test
    void projectsBreakEvenWhenCurrentlyLosingButRevenueIsCatchingUp() {
        // Still losing as of the latest month, but revenue is closing the gap with flat
        // expenses -> the projection should cross within the horizon.
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "500", "1000", null),
                snapshot("2026-02", "700", "1000", null),
                snapshot("2026-03", "900", "1000", null)));

        ForecastResponseDto result = forecastService.forecast(tenantId, 3);

        assertThat(result.breakEven().alreadyBreakEven()).isFalse();
        assertThat(result.breakEven().monthsFromNow()).isNotNull();
    }

    @Test
    void runwayIsUnavailableWithoutAKnownCashBalance() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "1000", "800", null),
                snapshot("2026-02", "1100", "850", null),
                snapshot("2026-03", "1200", "900", null)));

        ForecastResponseDto result = forecastService.forecast(tenantId, 3);

        assertThat(result.runway().available()).isFalse();
        assertThat(result.cash()).isNull();
    }

    @Test
    void projectsCashRunningOutWhenBurningCash() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(
                snapshot("2026-01", "500", "1500", new BigDecimal("2000")),
                snapshot("2026-02", "500", "1500", new BigDecimal("2000")),
                snapshot("2026-03", "500", "1500", new BigDecimal("2000"))));

        ForecastResponseDto result = forecastService.forecast(tenantId, 6);

        assertThat(result.runway().available()).isTrue();
        assertThat(result.runway().monthsUntilExhausted()).isNotNull();
        assertThat(result.cash()).isNotNull();
    }

    private MonthlyFinancialSnapshot snapshot(String period, String revenue, String expenses, BigDecimal cash) {
        BigDecimal rev = new BigDecimal(revenue);
        BigDecimal exp = new BigDecimal(expenses);
        MonthlyFinancialSnapshot snapshot = new MonthlyFinancialSnapshot(period, rev, exp, rev.subtract(exp), BigDecimal.ZERO);
        if (cash != null) {
            snapshot.applyCashPosition(cash, null);
        }
        return snapshot;
    }
}
