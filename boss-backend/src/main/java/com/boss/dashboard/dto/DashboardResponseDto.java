package com.boss.dashboard.dto;

import com.boss.analytics.dto.MonthlyComparisonDto;
import com.boss.analytics.dto.TrendDto;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.dto.CashFlowPointDto;
import com.boss.financial.dto.MonthlyBreakdownDto;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.risk.dto.RiskDto;

import java.util.List;

/**
 * Everything the dashboard needs in one round trip (section 26), so the frontend's
 * single screen loads with one request instead of a dozen. {@code hasData} is false
 * until the tenant's first upload - the frontend shows an empty state, never fabricated
 * numbers. {@code financeKpis} is independent of {@code hasData}: it comes from the
 * manually-entered Finance forms (Position/Profitability/Budget & Forecast/Capital),
 * a separate authoritative source from the transaction upload, and is populated
 * whenever those forms have data regardless of upload history.
 */
public record DashboardResponseDto(
        String tenantName,
        boolean hasData,
        MetricsDto metrics,
        MonthlyComparisonDto monthlyComparison,
        List<TrendDto> trends,
        List<MonthlyBreakdownDto> chartSeries,
        List<RiskDto> risks,
        ForecastResponseDto forecast,
        List<CashFlowPointDto> cashFlowSeries,
        FinanceKpiDto financeKpis
) {
}
