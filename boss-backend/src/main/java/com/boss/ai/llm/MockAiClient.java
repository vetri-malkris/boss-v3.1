package com.boss.ai.llm;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.dto.DiagnosisDto;
import com.boss.ai.dto.EvidenceItemDto;
import com.boss.ai.dto.RecommendationDto;
import com.boss.ai.orchestration.EvidenceContext;
import com.boss.analytics.dto.TrendDirection;
import com.boss.analytics.dto.TrendDto;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.dto.MetricsDto;
import com.boss.forecasting.dto.ForecastPointDto;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.risk.dto.RiskDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stands in for a real LLM provider when none is configured (default:
 * {@code boss.ai.provider=mock}). Every number in its answers comes straight from
 * {@link EvidenceContext} - it never calculates or invents a financial figure, only
 * selects and phrases the evidence the orchestrator already gathered (section 5/10 of
 * the "no fake AI behavior" requirement). Swap in a real {@code OpenAiClient} later
 * without touching {@code AiOrchestrator} or any caller.
 */
@Component
@ConditionalOnProperty(prefix = "boss.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {

    @Override
    public ChatResponseDto complete(String prompt, EvidenceContext context) {
        DashboardResponseDto dashboard = context.dashboard();

        if (!dashboard.hasData()) {
            return new ChatResponseDto(
                    "I don't have any financial data for your workspace yet. Upload a CSV or Excel file of "
                            + "transactions to get started.",
                    List.of(), null, List.of());
        }

        return switch (context.intent()) {
            case PROFIT -> profitResponse(dashboard);
            case REVENUE -> metricResponse(dashboard, "revenue", "Revenue", dashboard.metrics().revenue());
            case EXPENSES -> metricResponse(dashboard, "expenses", "Expenses", dashboard.metrics().expenses());
            case MARGIN -> metricResponse(dashboard, "margin", "Margin", dashboard.metrics().margin());
            case RUNWAY -> runwayResponse(dashboard);
            case RISKS -> risksResponse(dashboard);
            case FORECAST -> forecastResponse(context.forecast());
            case GENERAL -> generalResponse(dashboard);
        };
    }

    private ChatResponseDto forecastResponse(ForecastResponseDto forecast) {
        if (forecast == null || !forecast.hasEnoughData()) {
            return new ChatResponseDto(
                    "I need at least 3 months of uploaded data before I can forecast anything.",
                    List.of(), null, List.of());
        }

        ForecastPointDto nextRevenue = forecast.revenue().points().get(0);
        ForecastPointDto nextExpenses = forecast.expenses().points().get(0);
        StringBuilder summary = new StringBuilder();
        summary.append("Next month (").append(nextRevenue.period()).append("), projected revenue is ")
                .append(nextRevenue.value()).append(" and projected expenses are ").append(nextExpenses.value())
                .append(" (linear trend over your history).");

        List<EvidenceItemDto> evidence = new ArrayList<>();
        evidence.add(new EvidenceItemDto("Projected revenue (" + nextRevenue.period() + ")", nextRevenue.value().toString()));
        evidence.add(new EvidenceItemDto("Projected expenses (" + nextExpenses.period() + ")", nextExpenses.value().toString()));

        if (forecast.breakEven().alreadyBreakEven()) {
            summary.append(" You are already break-even or profitable.");
        } else if (forecast.breakEven().monthsFromNow() != null) {
            summary.append(" Projected break-even: ").append(forecast.breakEven().period())
                    .append(" (").append(forecast.breakEven().monthsFromNow()).append(" month(s) from now).");
            evidence.add(new EvidenceItemDto("Projected break-even", forecast.breakEven().period()));
        } else {
            summary.append(" At the current trend, break-even is not reached within the next ")
                    .append(forecast.horizonMonths()).append(" months.");
        }

        if (forecast.runway() != null && forecast.runway().available()) {
            if (forecast.runway().monthsUntilExhausted() != null) {
                summary.append(" Projected cash runway: ").append(forecast.runway().monthsUntilExhausted())
                        .append(" month(s), exhausted around ").append(forecast.runway().period()).append('.');
                evidence.add(new EvidenceItemDto("Projected cash exhausted", forecast.runway().period()));
            } else {
                summary.append(" Cash is projected to remain positive through the full forecast horizon.");
            }
        }

        return new ChatResponseDto(summary.toString(), evidence, null, List.of());
    }

    private ChatResponseDto profitResponse(DashboardResponseDto dashboard) {
        MetricsDto m = dashboard.metrics();
        boolean loss = m.profit().signum() < 0;
        String trendNote = trendNote(dashboard, "profit");

        String summary = loss
                ? "You operated at a loss of " + m.profit().abs() + " in " + m.period() + "." + trendNote
                : "Profit was " + m.profit() + " in " + m.period() + " (" + m.margin() + "% margin)." + trendNote;

        List<EvidenceItemDto> evidence = List.of(
                new EvidenceItemDto("Revenue (" + m.period() + ")", m.revenue().toString()),
                new EvidenceItemDto("Expenses (" + m.period() + ")", m.expenses().toString()),
                new EvidenceItemDto("Profit (" + m.period() + ")", m.profit().toString()),
                new EvidenceItemDto("Margin", m.margin() + "%"));

        DiagnosisDto diagnosis = loss
                ? new DiagnosisDto("Operating at a loss", "Expenses (" + m.expenses() + ") exceeded revenue ("
                        + m.revenue() + ") in " + m.period() + ".", "Reduces cash reserves by " + m.profit().abs()
                        + " for the period.")
                : null;

        List<RecommendationDto> recommendations = loss
                ? List.of(new RecommendationDto(
                        "Review the largest expense categories for near-term cost reduction opportunities.",
                        "Narrowing the gap between revenue and expenses.",
                        0.4,
                        List.of("Cost structure stays broadly similar next period."),
                        List.of("Grow revenue instead of cutting costs.", "Raise prices."),
                        List.of("Cutting costs too aggressively can affect quality or growth.")))
                : List.of();

        return new ChatResponseDto(summary, evidence, diagnosis, recommendations);
    }

    private ChatResponseDto metricResponse(DashboardResponseDto dashboard, String metricKey, String label,
                                            BigDecimal value) {
        MetricsDto m = dashboard.metrics();
        String trendNote = trendNote(dashboard, metricKey);
        String suffix = "margin".equals(metricKey) ? "%" : "";
        String summary = label + " was " + value + suffix + " in " + m.period() + "." + trendNote;

        List<EvidenceItemDto> evidence = new ArrayList<>();
        evidence.add(new EvidenceItemDto(label + " (" + m.period() + ")", value + suffix));
        Optional.ofNullable(dashboard.monthlyComparison()).ifPresent(comparison -> comparison.changes().stream()
                .filter(c -> c.metric().equals(metricKey))
                .findFirst()
                .ifPresent(c -> evidence.add(new EvidenceItemDto(
                        "Change vs " + comparison.previousPeriod(), c.change() + " (" + c.changePercentage() + "%)"))));

        return new ChatResponseDto(summary, evidence, null, List.of());
    }

    private ChatResponseDto runwayResponse(DashboardResponseDto dashboard) {
        MetricsDto m = dashboard.metrics();
        if (m.runwayMonths() == null) {
            return new ChatResponseDto(
                    "Runway isn't available yet - upload a file with a starting cash balance, or your business "
                            + "isn't currently burning cash.",
                    List.of(), null, List.of());
        }

        boolean short_ = m.runwayMonths().compareTo(BigDecimal.valueOf(6)) < 0;
        String summary = "You have " + m.runwayMonths() + " months of runway at the current burn rate."
                + (short_ ? " That's under the 6-month safety threshold." : "");

        List<EvidenceItemDto> evidence = List.of(
                new EvidenceItemDto("Cash balance", String.valueOf(m.cashBalance())),
                new EvidenceItemDto("Runway", m.runwayMonths() + " months"));

        DiagnosisDto diagnosis = short_
                ? new DiagnosisDto("Short cash runway", "Monthly burn exceeds what current cash can sustain long-term.",
                        "Risk of running out of cash within " + m.runwayMonths() + " months if unchanged.")
                : null;

        return new ChatResponseDto(summary, evidence, diagnosis, List.of());
    }

    private ChatResponseDto risksResponse(DashboardResponseDto dashboard) {
        List<RiskDto> risks = dashboard.risks();
        if (risks.isEmpty()) {
            return new ChatResponseDto("No risks were detected for " + dashboard.metrics().period() + ".",
                    List.of(), null, List.of());
        }

        RiskDto top = risks.get(0);
        String summary = risks.size() + " risk(s) detected for " + dashboard.metrics().period()
                + ". The most severe: " + top.risk() + " (" + top.severity() + ").";

        List<EvidenceItemDto> evidence = risks.stream()
                .map(r -> new EvidenceItemDto(r.risk() + " (" + r.severity() + ")", r.evidence()))
                .toList();

        DiagnosisDto diagnosis = new DiagnosisDto(top.risk(), top.possibleCause(), top.financialImpact());

        return new ChatResponseDto(summary, evidence, diagnosis, List.of());
    }

    private ChatResponseDto generalResponse(DashboardResponseDto dashboard) {
        MetricsDto m = dashboard.metrics();
        String summary = "I can answer questions about revenue, expenses, profit, margin, runway, and risks based on "
                + "your uploaded data - try \"why did profit decrease\" or \"what is our runway\".";

        List<EvidenceItemDto> evidence = List.of(
                new EvidenceItemDto("Revenue (" + m.period() + ")", m.revenue().toString()),
                new EvidenceItemDto("Expenses (" + m.period() + ")", m.expenses().toString()),
                new EvidenceItemDto("Profit (" + m.period() + ")", m.profit().toString()));

        return new ChatResponseDto(summary, evidence, null, List.of());
    }

    private String trendNote(DashboardResponseDto dashboard, String metric) {
        return dashboard.trends().stream()
                .filter(t -> t.metric().equals(metric))
                .findFirst()
                .map(this::describeTrend)
                .orElse("");
    }

    private String describeTrend(TrendDto trend) {
        if (trend.direction() == TrendDirection.STABLE) {
            return "";
        }
        String verb = switch (trend.direction()) {
            case INCREASING -> "increasing";
            case DECREASING -> "decreasing";
            case VOLATILE -> "fluctuating";
            case STABLE -> "";
        };
        return " It has been " + verb + " over the last " + trend.monthsObserved() + " months.";
    }
}
