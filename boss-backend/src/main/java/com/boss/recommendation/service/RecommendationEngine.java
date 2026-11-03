package com.boss.recommendation.service;

import com.boss.analytics.dto.TrendDto;
import com.boss.analytics.service.TrendDetectionEngine;
import com.boss.finance.gst.service.GstService;
import com.boss.finance.kpi.FinanceKpiService;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.recommendation.domain.EvidenceEntry;
import com.boss.recommendation.domain.Recommendation;
import com.boss.recommendation.repository.RecommendationRepository;
import com.boss.risk.dto.RiskDto;
import com.boss.risk.dto.WorkingCapitalDto;
import com.boss.risk.service.RiskEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Translates each risk from {@link RiskEngine} into one concrete, actionable
 * recommendation (section 7/15). Every field traces back to the risk it came from -
 * this is template-driven Java, not an LLM call, so nothing here can invent a number
 * or a claim the risk engine didn't already establish. Confidence values are
 * deliberately moderate (0.45-0.65): the diagnosis is a fact, but "this specific
 * remedy will work" is inherently uncertain.
 */
@Service
public class RecommendationEngine {

    private final SnapshotService snapshotService;
    private final TrendDetectionEngine trendDetectionEngine;
    private final RiskEngine riskEngine;
    private final RecommendationRepository repository;
    private final GstService gstService;
    private final FinanceKpiService financeKpiService;
    private final Map<String, RecommendationTemplate> templates = buildTemplates();

    public RecommendationEngine(SnapshotService snapshotService, TrendDetectionEngine trendDetectionEngine,
                                 RiskEngine riskEngine, RecommendationRepository repository,
                                 GstService gstService, FinanceKpiService financeKpiService) {
        this.snapshotService = snapshotService;
        this.trendDetectionEngine = trendDetectionEngine;
        this.riskEngine = riskEngine;
        this.repository = repository;
        this.gstService = gstService;
        this.financeKpiService = financeKpiService;
    }

    @Transactional
    public List<Recommendation> generate(UUID tenantId) {
        List<MonthlyFinancialSnapshot> history = snapshotService.history(tenantId);
        FinanceKpiDto financeKpis = financeKpiService.summarize(tenantId);

        if (history.isEmpty()) {
            // No transaction upload yet, but the Finance forms are a separate
            // authoritative source - their own risks still deserve recommendations.
            return buildAndSaveRecommendations(tenantId, YearMonth.now().toString(),
                    riskEngine.evaluateFinanceKpis(financeKpis));
        }

        MonthlyFinancialSnapshot latest = history.get(history.size() - 1);
        List<TrendDto> trends = trendDetectionEngine.detectTrends(history);
        BigDecimal receivables = financeKpiService.receivables(tenantId);
        BigDecimal payables = financeKpiService.payables(tenantId);
        BigDecimal gstLiability = gstService.currentLiability(tenantId);
        WorkingCapitalDto workingCapital = new WorkingCapitalDto(
                receivables == null ? BigDecimal.ZERO : receivables,
                payables == null ? BigDecimal.ZERO : payables,
                gstLiability == null ? BigDecimal.ZERO : gstLiability);
        List<RiskDto> risks = riskEngine.evaluate(latest, trends, workingCapital, financeKpis);
        return buildAndSaveRecommendations(tenantId, latest.getPeriod(), risks);
    }

    private List<Recommendation> buildAndSaveRecommendations(UUID tenantId, String period, List<RiskDto> risks) {
        List<Recommendation> created = new ArrayList<>();
        for (RiskDto risk : risks) {
            RecommendationTemplate template = templates.get(risk.risk());
            if (template == null) {
                continue; // no canned guidance yet for this risk type - don't fabricate one
            }
            created.add(buildRecommendation(tenantId, period, risk, template));
        }
        return repository.saveAll(created);
    }

    @Transactional(readOnly = true)
    public List<Recommendation> list(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private Recommendation buildRecommendation(UUID tenantId, String period, RiskDto risk, RecommendationTemplate template) {
        Recommendation recommendation = new Recommendation(period, risk.risk(), risk.risk(), risk.possibleCause(),
                risk.financialImpact(), template.recommendedAction(), template.expectedOutcome(),
                template.confidence(), template.timeHorizon(), template.followUpMeasurement());
        recommendation.setTenantId(tenantId);
        recommendation.setEvidence(List.of(
                new EvidenceEntry(risk.risk(), risk.evidence()),
                new EvidenceEntry("Financial impact", risk.financialImpact())));
        recommendation.setAlternatives(template.alternatives());
        recommendation.setRisks(template.risksOfAction());
        recommendation.setAssumptions(template.assumptions());
        return recommendation;
    }

    private record RecommendationTemplate(String recommendedAction, List<String> alternatives, String expectedOutcome,
                                           List<String> risksOfAction, BigDecimal confidence, List<String> assumptions,
                                           String timeHorizon, String followUpMeasurement) {
    }

    private Map<String, RecommendationTemplate> buildTemplates() {
        return Map.of(
                "Operating at a loss", new RecommendationTemplate(
                        "Review the largest expense categories for near-term cost reduction, and identify which "
                                + "revenue lines can be grown fastest.",
                        List.of("Raise prices on top-selling items/services", "Renegotiate vendor contracts",
                                "Pause non-essential hiring"),
                        "Narrow the gap between revenue and expenses within 1-2 months.",
                        List.of("Cutting costs too aggressively can hurt growth or service quality."),
                        new BigDecimal("0.55"),
                        List.of("Recent revenue/expense trend continues unless acted upon.",
                                "No one-off event explains this period's loss."),
                        "1-2 months", "Re-check profit in next month's upload."),

                "Short cash runway", new RecommendationTemplate(
                        "Prioritize extending runway: cut discretionary spend immediately and evaluate options to "
                                + "raise additional cash (financing, collections, or a bridge).",
                        List.of("Accelerate receivables collection", "Negotiate extended payment terms with vendors",
                                "Raise a bridge round or short-term financing"),
                        "Extend runway beyond the 6-month safety threshold.",
                        List.of("Raising financing under pressure may come on worse terms."),
                        new BigDecimal("0.6"),
                        List.of("Current burn rate continues unless spend is cut."),
                        "Immediate (this month)", "Re-check runway after your next upload with an updated cash balance."),

                "Revenue decline", new RecommendationTemplate(
                        "Investigate the root cause of the revenue decline (churn, pricing, demand) via customer/"
                                + "sales data before deciding on a remedy.",
                        List.of("Run a customer win-back campaign", "Review pricing against competitors",
                                "Expand into an adjacent customer segment"),
                        "Identify the primary driver of the decline within the next reporting period.",
                        List.of("Delaying action while investigating allows the decline to continue."),
                        new BigDecimal("0.45"),
                        List.of("The decline reflects a business issue, not a one-off timing effect."),
                        "2-3 months", "Track revenue trend direction over the next 2-3 uploads."),

                "Margin compression", new RecommendationTemplate(
                        "Review pricing and cost structure - compare unit economics against a prior healthy period "
                                + "to find where margin is leaking.",
                        List.of("Renegotiate input/vendor costs", "Reprice underpriced offerings",
                                "Cut low-margin product/service lines"),
                        "Stabilize or improve margin over the next 2-3 months.",
                        List.of("Repricing can affect demand or customer retention."),
                        new BigDecimal("0.55"),
                        List.of("Cost or pricing mix, not a one-off event, is driving the compression."),
                        "2-3 months", "Re-check margin trend over the next 2-3 uploads."),

                "Rapid expense growth", new RecommendationTemplate(
                        "Audit recent expense growth by category to confirm it's intentional (e.g. planned hiring) "
                                + "rather than uncontrolled cost creep.",
                        List.of("Freeze discretionary spend pending the audit", "Set department-level budgets",
                                "Review new vendor/subscription spend"),
                        "Confirm expense growth aligns with a deliberate growth plan, or curb it.",
                        List.of("Freezing spend prematurely could stall a legitimate growth initiative."),
                        new BigDecimal("0.6"),
                        List.of("Expense growth is broad-based rather than one large planned item."),
                        "1 month", "Re-check expense trend in next month's upload."),

                "Receivables high relative to revenue", new RecommendationTemplate(
                        "Prioritise collection outreach on your largest outstanding accounts before pursuing new sales.",
                        List.of("Offer an early-payment discount", "Escalate long-outstanding accounts to a collections process",
                                "Tighten credit terms for new invoices"),
                        "Convert outstanding receivables into collected cash within the next billing cycle.",
                        List.of("Aggressive collection tactics can strain customer relationships."),
                        new BigDecimal("0.5"),
                        List.of("Customers are able to pay but have deprioritized these invoices, not disputing them."),
                        "2-4 weeks", "Re-check the outstanding receivables total after the next collection push."),

                "Payables may exceed available cash", new RecommendationTemplate(
                        "Sequence upcoming supplier payments against expected cash inflows, and contact key suppliers "
                                + "about extended terms if a shortfall is likely.",
                        List.of("Negotiate extended payment terms with the largest suppliers",
                                "Delay non-critical purchases", "Accelerate receivables collection to cover the gap"),
                        "Meet payment obligations without breaching the safe cash buffer.",
                        List.of("Delaying supplier payments can damage vendor relationships or credit terms."),
                        new BigDecimal("0.55"),
                        List.of("The cash balance and payables balance used are current as of the latest data."),
                        "Immediate (this month)", "Re-check the cash balance against payables after acting."),

                "GST liability outstanding", new RecommendationTemplate(
                        "Confirm the GST return has been filed for the period and schedule payment of the net liability.",
                        List.of("Set aside the liability amount in a separate account ahead of the due date"),
                        "Clear the outstanding GST liability before it accrues interest or penalties.",
                        List.of("None beyond the cash outflow itself."),
                        new BigDecimal("0.65"),
                        List.of("The net liability is correctly computed as output tax minus input tax credit."),
                        "Before the filing due date", "Confirm the liability is marked paid after payment."),

                "Low current ratio (Financial Position)", new RecommendationTemplate(
                        "Review the current assets and liabilities entered under Financial Position - accelerate "
                                + "receivables collection or reduce short-term liabilities to restore a healthy buffer.",
                        List.of("Negotiate extended terms on short-term liabilities", "Convert excess inventory to cash",
                                "Arrange a short-term credit facility"),
                        "Bring the current ratio back above 1.0 within the next reporting period.",
                        List.of("Aggressively liquidating inventory or receivables can affect operations or customer relationships."),
                        new BigDecimal("0.5"),
                        List.of("The Financial Position entry used is current and complete."),
                        "1-2 months", "Re-check the current ratio after the next Financial Position entry."),

                "Net loss reported (Profitability)", new RecommendationTemplate(
                        "Review the Revenue, COGS, Operating Expenses, Finance Cost, and Tax entered under "
                                + "Profitability to identify the primary driver of the loss before deciding on a remedy.",
                        List.of("Reprice underperforming offerings", "Reduce discretionary operating expenses",
                                "Review finance cost - refinancing may reduce interest burden"),
                        "Identify the primary driver of the loss and return to profitability within 1-2 periods.",
                        List.of("Cutting costs too aggressively can hurt growth or service quality."),
                        new BigDecimal("0.45"),
                        List.of("The Profitability entry used reflects the full period, not a partial one."),
                        "1-2 months", "Re-check net profit after the next Profitability entry.")
        );
    }
}
