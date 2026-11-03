package com.boss.risk.service;

import com.boss.analytics.dto.TrendDirection;
import com.boss.analytics.dto.TrendDto;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.risk.dto.EvidenceType;
import com.boss.risk.dto.RiskDto;
import com.boss.risk.dto.RiskSeverity;
import com.boss.risk.dto.WorkingCapitalDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic, rule-based risk checks over the latest snapshot and detected trends
 * (section 10). This is the whole risk engine for now - no ML/AI involved, every rule
 * is a plain threshold or trend-direction check, and every risk's {@code evidence} is
 * labeled FACT because it is read directly off computed numbers, never guessed.
 * {@code possibleCause} is always a hypothesis, never asserted as confirmed.
 */
@Service
public class RiskEngine {

    private static final BigDecimal LOW_MARGIN_THRESHOLD = BigDecimal.valueOf(10);
    private static final BigDecimal LOW_RUNWAY_MONTHS = BigDecimal.valueOf(6);
    private static final BigDecimal AR_HIGH_THRESHOLD_PERCENT_OF_REVENUE = BigDecimal.valueOf(20);
    private static final BigDecimal LOW_CURRENT_RATIO = BigDecimal.ONE;

    /**
     * Same as {@link #evaluate(MonthlyFinancialSnapshot, List, WorkingCapitalDto)} plus
     * {@link #evaluateFinanceKpis(FinanceKpiDto)} - the manually-entered Finance forms
     * are a separate authoritative source from the transaction-upload snapshot, so
     * their risks are worth checking every time this overload runs.
     */
    public List<RiskDto> evaluate(MonthlyFinancialSnapshot latest, List<TrendDto> trends,
                                   WorkingCapitalDto workingCapital, FinanceKpiDto financeKpis) {
        List<RiskDto> risks = new ArrayList<>(evaluate(latest, trends, workingCapital));
        risks.addAll(evaluateFinanceKpis(financeKpis));
        return risks;
    }

    /**
     * Checks over the manually-entered Finance forms (Financial Position,
     * Profitability) alone - deliberately independent of any transaction-upload
     * snapshot, since a tenant can fill in these forms without ever uploading a
     * transaction file. Named to make that source unambiguous rather than reusing
     * "Operating at a loss"/similar names already used for the snapshot-derived risks.
     */
    public List<RiskDto> evaluateFinanceKpis(FinanceKpiDto financeKpis) {
        List<RiskDto> risks = new ArrayList<>();

        BigDecimal currentRatio = financeKpis.currentRatio();
        if (currentRatio != null && currentRatio.compareTo(LOW_CURRENT_RATIO) < 0) {
            risks.add(new RiskDto(
                    "Low current ratio (Financial Position)",
                    RiskSeverity.HIGH,
                    "Current ratio is " + currentRatio + " based on the latest Financial Position entry.",
                    EvidenceType.FACT,
                    "Current liabilities exceed current assets - short-term obligations may not be fully covered.",
                    "Receivables/inventory are tying up cash, or short-term liabilities have grown faster than assets."));
        }

        BigDecimal netProfit = financeKpis.netProfit();
        if (netProfit != null && netProfit.signum() < 0) {
            risks.add(new RiskDto(
                    "Net loss reported (Profitability)",
                    RiskSeverity.HIGH,
                    "Net profit is " + netProfit + " based on the latest Profitability entry.",
                    EvidenceType.FACT,
                    "The business is reporting a net loss for the period on a cost/tax-inclusive basis.",
                    "Revenue, COGS, operating expenses, finance cost, or tax may need review."));
        }

        return risks;
    }

    /**
     * Same as {@link #evaluate(MonthlyFinancialSnapshot, List)} plus working-capital
     * checks (receivables high relative to revenue, payables vs. available cash,
     * outstanding GST liability) - section 15/16's "AR -> cash pressure" example made
     * concrete, sourced from Position's own current balance (no due-date detail, so
     * these check the aggregate balance rather than aging).
     */
    public List<RiskDto> evaluate(MonthlyFinancialSnapshot latest, List<TrendDto> trends, WorkingCapitalDto workingCapital) {
        List<RiskDto> risks = new ArrayList<>(evaluate(latest, trends));

        BigDecimal receivablesOutstanding = workingCapital.receivablesOutstanding();
        if (receivablesOutstanding.signum() > 0 && exceedsRevenueShare(receivablesOutstanding, latest.getRevenue())) {
            risks.add(new RiskDto(
                    "Receivables high relative to revenue",
                    RiskSeverity.HIGH,
                    receivablesOutstanding + " in outstanding receivables against " + latest.getRevenue() + " revenue.",
                    EvidenceType.FACT,
                    "Cash that revenue already counted as earned has not yet been collected.",
                    "Collections are slowing, or a customer dispute is holding up payment."));
        }

        BigDecimal payablesOutstanding = workingCapital.payablesOutstanding();
        if (latest.getCashBalance() != null && payablesOutstanding.compareTo(latest.getCashBalance()) > 0) {
            risks.add(new RiskDto(
                    "Payables may exceed available cash",
                    RiskSeverity.HIGH,
                    payablesOutstanding + " in outstanding payables against a cash balance of " + latest.getCashBalance() + ".",
                    EvidenceType.FACT,
                    "Meeting these obligations may require drawing down cash below a safe buffer.",
                    "Outgoing obligations are large relative to available cash."));
        }

        BigDecimal gstLiability = workingCapital.gstLiability();
        if (gstLiability.signum() > 0) {
            risks.add(new RiskDto(
                    "GST liability outstanding",
                    RiskSeverity.MEDIUM,
                    gstLiability + " in net GST liability is unpaid.",
                    EvidenceType.FACT,
                    "This is a scheduled cash outflow that has not yet been paid.",
                    "Filing is pending, or payment has not been scheduled yet."));
        }

        return risks;
    }

    private boolean exceedsRevenueShare(BigDecimal amount, BigDecimal revenue) {
        if (revenue == null || revenue.signum() <= 0) {
            return true;
        }
        BigDecimal percentOfRevenue = amount.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, java.math.RoundingMode.HALF_UP);
        return percentOfRevenue.compareTo(AR_HIGH_THRESHOLD_PERCENT_OF_REVENUE) > 0;
    }

    public List<RiskDto> evaluate(MonthlyFinancialSnapshot latest, List<TrendDto> trends) {
        List<RiskDto> risks = new ArrayList<>();
        Map<String, TrendDto> trendByMetric = trends.stream()
                .collect(java.util.stream.Collectors.toMap(TrendDto::metric, t -> t));

        if (latest.getProfit().signum() < 0) {
            risks.add(new RiskDto(
                    "Operating at a loss",
                    RiskSeverity.HIGH,
                    "Profit was " + latest.getProfit() + " in " + latest.getPeriod() + ".",
                    EvidenceType.FACT,
                    "Cash reserves are depleted by " + latest.getProfit().abs() + " per month at this rate.",
                    "Expenses exceeded revenue this period."));
        } else if (latest.getRevenue().signum() > 0 && latest.getMargin().compareTo(LOW_MARGIN_THRESHOLD) < 0) {
            risks.add(new RiskDto(
                    "Low profit margin",
                    RiskSeverity.MEDIUM,
                    "Margin was " + latest.getMargin() + "% in " + latest.getPeriod() + ".",
                    EvidenceType.FACT,
                    "Limited buffer to absorb a revenue drop or cost increase.",
                    "Pricing, discounting, or cost structure may need review."));
        }

        if (latest.getRunwayMonths() != null && latest.getRunwayMonths().compareTo(LOW_RUNWAY_MONTHS) < 0) {
            risks.add(new RiskDto(
                    "Short cash runway",
                    RiskSeverity.HIGH,
                    "Runway is " + latest.getRunwayMonths() + " months at the current burn rate.",
                    EvidenceType.FACT,
                    "Cash could be exhausted within " + latest.getRunwayMonths() + " months if the trend continues.",
                    "Burn rate is currently higher than incoming revenue can sustain."));
        }

        trendRisk(trendByMetric, "revenue", TrendDirection.DECREASING, risks,
                "Revenue decline",
                months -> "Revenue has decreased over the last " + months + " months.",
                "Reduced cash inflow may compress margin and shorten runway.",
                "Possible cause (not confirmed): demand slowdown, customer churn, or seasonality.");

        trendRisk(trendByMetric, "margin", TrendDirection.DECREASING, risks,
                "Margin compression",
                months -> "Profit margin has decreased over the last " + months + " months.",
                "Each additional unit of revenue now converts to less profit.",
                "Possible cause (not confirmed): rising costs, discounting, or an unfavorable revenue mix.");

        trendRisk(trendByMetric, "expenses", TrendDirection.INCREASING, risks,
                "Rapid expense growth",
                months -> "Expenses have increased over the last " + months + " months.",
                "Continued growth at this rate will erode profit if revenue does not keep pace.",
                "Possible cause (not confirmed): hiring, vendor cost increases, or one-off spend.");

        return risks;
    }

    private void trendRisk(Map<String, TrendDto> trendByMetric, String metric, TrendDirection watchFor,
                            List<RiskDto> sink, String riskName, java.util.function.IntFunction<String> evidence,
                            String financialImpact, String possibleCause) {
        Optional.ofNullable(trendByMetric.get(metric))
                .filter(t -> t.direction() == watchFor)
                .ifPresent(t -> sink.add(new RiskDto(
                        riskName,
                        t.monthsObserved() >= 4 ? RiskSeverity.HIGH : RiskSeverity.MEDIUM,
                        evidence.apply(t.monthsObserved()),
                        EvidenceType.FACT,
                        financialImpact,
                        possibleCause)));
    }
}
