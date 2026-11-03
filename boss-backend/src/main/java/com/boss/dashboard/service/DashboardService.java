package com.boss.dashboard.service;

import com.boss.analytics.dto.TrendDto;
import com.boss.analytics.service.MonthComparisonService;
import com.boss.analytics.service.TrendDetectionEngine;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.dto.MetricsDto;
import com.boss.financial.calculator.CashFlowCalculator;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.dto.CashFlowPointDto;
import com.boss.financial.dto.MonthlyBreakdownDto;
import com.boss.financial.service.SnapshotService;
import com.boss.finance.gst.service.GstService;
import com.boss.finance.kpi.FinanceKpiService;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.service.ForecastService;
import com.boss.risk.dto.RiskDto;
import com.boss.risk.dto.WorkingCapitalDto;
import com.boss.risk.service.RiskEngine;
import com.boss.tenant.domain.Tenant;
import com.boss.tenant.service.TenantService;
import com.boss.workforce.service.WorkforceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Composes financial history, analytics, risk and forecast into the single payload
 * the dashboard screen renders (section 26/27). A thin façade over the domain
 * modules - not itself a module that would ever be extracted into a microservice.
 */
@Service
public class DashboardService {

    private static final int DEFAULT_FORECAST_HORIZON_MONTHS = 12;

    private final SnapshotService snapshotService;
    private final MonthComparisonService monthComparisonService;
    private final TrendDetectionEngine trendDetectionEngine;
    private final RiskEngine riskEngine;
    private final ForecastService forecastService;
    private final TenantService tenantService;
    private final CashFlowCalculator cashFlowCalculator;
    private final GstService gstService;
    private final WorkforceService workforceService;
    private final FinanceKpiService financeKpiService;

    public DashboardService(SnapshotService snapshotService, MonthComparisonService monthComparisonService,
                             TrendDetectionEngine trendDetectionEngine, RiskEngine riskEngine,
                             ForecastService forecastService, TenantService tenantService,
                             CashFlowCalculator cashFlowCalculator, GstService gstService,
                             WorkforceService workforceService, FinanceKpiService financeKpiService) {
        this.snapshotService = snapshotService;
        this.monthComparisonService = monthComparisonService;
        this.trendDetectionEngine = trendDetectionEngine;
        this.riskEngine = riskEngine;
        this.forecastService = forecastService;
        this.tenantService = tenantService;
        this.cashFlowCalculator = cashFlowCalculator;
        this.gstService = gstService;
        this.workforceService = workforceService;
        this.financeKpiService = financeKpiService;
    }

    @Transactional(readOnly = true)
    public DashboardResponseDto build(UUID tenantId) {
        Tenant tenant = tenantService.getById(tenantId);
        List<MonthlyFinancialSnapshot> history = snapshotService.history(tenantId);
        FinanceKpiDto financeKpis = financeKpiService.summarize(tenantId);
        MonthlyFinancialSnapshot latest = history.isEmpty() ? null : history.get(history.size() - 1);

        List<CashFlowPointDto> cashFlowSeries = cashFlowCalculator.calculate(history);
        BigDecimal netCashFlow = cashFlowSeries.isEmpty() ? null : cashFlowSeries.get(cashFlowSeries.size() - 1).netChange();

        BigDecimal receivables = financeKpiService.receivables(tenantId);
        BigDecimal payables = financeKpiService.payables(tenantId);
        BigDecimal outputGst = gstService.outputGst(tenantId);
        BigDecimal inputGst = gstService.inputGst(tenantId);
        BigDecimal gstLiability = gstService.currentLiability(tenantId);
        BigDecimal monthlyPeopleCost = workforceService.monthlyPeopleCost(tenantId);

        MetricsDto metrics = new MetricsDto(
                latest == null ? null : latest.getPeriod(),
                latest == null ? null : latest.getRevenue(),
                latest == null ? null : latest.getExpenses(),
                latest == null ? null : latest.getProfit(),
                latest == null ? null : latest.getMargin(),
                latest == null ? null : latest.getCashBalance(),
                latest == null ? null : latest.getRunwayMonths(),
                receivables, payables, netCashFlow, gstLiability, outputGst, inputGst, monthlyPeopleCost);

        List<TrendDto> trends = trendDetectionEngine.detectTrends(history);
        // RiskEngine's checks assume non-null inputs (it never null-checks before
        // signum()) - "not entered yet" (null, shown as N/A on Overview) becomes "zero"
        // only for the purpose of risk evaluation, never fabricated back into metrics.
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(
                receivables == null ? BigDecimal.ZERO : receivables,
                payables == null ? BigDecimal.ZERO : payables,
                gstLiability == null ? BigDecimal.ZERO : gstLiability);
        // RiskEngine's main evaluate() reads the latest snapshot's own fields, so it
        // only runs once one exists - Finance-form risks (which don't depend on any
        // snapshot) always run regardless, same as financeKpis itself.
        List<RiskDto> risks = latest == null
                ? riskEngine.evaluateFinanceKpis(financeKpis)
                : riskEngine.evaluate(latest, trends, workingCapital, financeKpis);
        List<MonthlyBreakdownDto> chartSeries = history.stream()
                .map(s -> new MonthlyBreakdownDto(s.getPeriod(), s.getRevenue(), s.getExpenses(), s.getProfit()))
                .toList();
        ForecastResponseDto forecast = forecastService.forecast(tenantId, DEFAULT_FORECAST_HORIZON_MONTHS);

        return new DashboardResponseDto(tenant.getName(), latest != null, metrics,
                monthComparisonService.compare(history).orElse(null), trends, chartSeries, risks, forecast,
                cashFlowSeries, financeKpis);
    }
}
