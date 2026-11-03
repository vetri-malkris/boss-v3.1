package com.boss.ai.llm;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.orchestration.EvidenceContext;
import com.boss.ai.orchestration.Intent;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.dto.MetricsDto;
import com.boss.forecasting.dto.BreakEvenForecastDto;
import com.boss.forecasting.dto.ForecastPointDto;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.dto.MetricForecastDto;
import com.boss.forecasting.dto.RunwayForecastDto;
import com.boss.risk.dto.EvidenceType;
import com.boss.risk.dto.RiskDto;
import com.boss.risk.dto.RiskSeverity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiClientTest {

    private final MockAiClient client = new MockAiClient();

    @Test
    void tellsTheUserToUploadDataWhenNoneExists() {
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", false, null, null, List.of(), List.of(), List.of(), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("hi", Intent.GENERAL, dashboard, null));

        assertThat(response.summary()).containsIgnoringCase("upload");
        assertThat(response.evidence()).isEmpty();
    }

    @Test
    void everyNumberInAProfitAnswerTracesBackToTheMetrics() {
        MetricsDto metrics = new MetricsDto("2026-01", new BigDecimal("1000.00"), new BigDecimal("1500.00"),
                new BigDecimal("-500.00"), new BigDecimal("-50.00"), null, null, null, null, null, null, null, null, null);
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("why did profit decrease", Intent.PROFIT, dashboard, null));

        assertThat(response.summary()).contains("500.00");
        assertThat(response.diagnosis()).isNotNull();
        assertThat(response.diagnosis().issue()).isEqualTo("Operating at a loss");
        assertThat(response.evidence()).anySatisfy(e -> assertThat(e.value()).isEqualTo("1000.00"));
        assertThat(response.evidence()).anySatisfy(e -> assertThat(e.value()).isEqualTo("1500.00"));
    }

    @Test
    void positiveProfitProducesNoDiagnosis() {
        MetricsDto metrics = new MetricsDto("2026-01", new BigDecimal("5000.00"), new BigDecimal("2000.00"),
                new BigDecimal("3000.00"), new BigDecimal("60.00"), null, null, null, null, null, null, null, null, null);
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("how's profit", Intent.PROFIT, dashboard, null));

        assertThat(response.diagnosis()).isNull();
        assertThat(response.recommendations()).isEmpty();
    }

    @Test
    void runwayResponseExplainsWhenNotAvailable() {
        MetricsDto metrics = new MetricsDto("2026-01", new BigDecimal("1000"), new BigDecimal("500"),
                new BigDecimal("500"), new BigDecimal("50"), null, null, null, null, null, null, null, null, null);
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("what's our runway", Intent.RUNWAY, dashboard, null));

        assertThat(response.summary()).containsIgnoringCase("available");
        assertThat(response.evidence()).isEmpty();
    }

    @Test
    void risksResponseSurfacesTheMostSevereRiskAsDiagnosis() {
        MetricsDto metrics = new MetricsDto("2026-01", new BigDecimal("1000"), new BigDecimal("2000"),
                new BigDecimal("-1000"), new BigDecimal("-100"), null, null, null, null, null, null, null, null, null);
        RiskDto risk = new RiskDto("Operating at a loss", RiskSeverity.HIGH, "Profit was -1000.00", EvidenceType.FACT,
                "Cash depleted by 1000.00/month", "Expenses exceeded revenue");
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(risk), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("any risks?", Intent.RISKS, dashboard, null));

        assertThat(response.diagnosis().issue()).isEqualTo("Operating at a loss");
        assertThat(response.evidence()).hasSize(1);
    }

    @Test
    void forecastResponseNeedsMoreDataWhenForecastIsAbsent() {
        MetricsDto metrics = new MetricsDto("2026-01", new BigDecimal("1000"), new BigDecimal("500"),
                new BigDecimal("500"), new BigDecimal("50"), null, null, null, null, null, null, null, null, null);
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(), null, List.of(), null);

        ChatResponseDto response = client.complete("prompt", new EvidenceContext("forecast our revenue", Intent.FORECAST, dashboard, null));

        assertThat(response.summary()).containsIgnoringCase("at least 3 months");
    }

    @Test
    void forecastResponseSurfacesNextMonthAndBreakEven() {
        MetricsDto metrics = new MetricsDto("2026-03", new BigDecimal("1000"), new BigDecimal("2000"),
                new BigDecimal("-1000"), new BigDecimal("-100"), null, null, null, null, null, null, null, null, null);
        DashboardResponseDto dashboard = new DashboardResponseDto("Acme", true, metrics, null, List.of(), List.of(), List.of(), null, List.of(), null);

        MetricForecastDto revenue = new MetricForecastDto("revenue", List.of(new ForecastPointDto("2026-04", new BigDecimal("1100.00"))));
        MetricForecastDto expenses = new MetricForecastDto("expenses", List.of(new ForecastPointDto("2026-04", new BigDecimal("1900.00"))));
        BreakEvenForecastDto breakEven = new BreakEvenForecastDto(false, 3, "2026-06");
        RunwayForecastDto runway = new RunwayForecastDto(false, null, null);
        ForecastResponseDto forecast = new ForecastResponseDto(12, true, revenue, expenses, null, runway, breakEven);

        ChatResponseDto response = client.complete("prompt",
                new EvidenceContext("what's our forecast", Intent.FORECAST, dashboard, forecast));

        assertThat(response.summary()).contains("1100.00").contains("1900.00").contains("2026-06");
    }
}
