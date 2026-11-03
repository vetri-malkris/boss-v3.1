package com.boss.ai.prompt;

import com.boss.ai.orchestration.EvidenceContext;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.dto.MetricsDto;
import com.boss.risk.dto.RiskDto;
import org.springframework.stereotype.Component;

/**
 * Renders the evidence gathered by the orchestrator into a prompt a real LLM would
 * consume. {@link com.boss.ai.llm.MockAiClient} ignores this (it reasons over the
 * structured {@link EvidenceContext} directly) - it exists so a future
 * {@code OpenAiClient} only needs to send this string, not re-implement evidence
 * formatting (section 13).
 */
@Component
public class PromptBuilder {

    public String build(EvidenceContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are BOSS, an AI CFO. Answer only using the evidence below; never invent numbers.\n\n");
        prompt.append("User question: ").append(context.userMessage()).append('\n');
        prompt.append("Detected intent: ").append(context.intent()).append("\n\n");

        DashboardResponseDto dashboard = context.dashboard();
        if (!dashboard.hasData()) {
            prompt.append("No financial data has been uploaded for this tenant yet.\n");
            return prompt.toString();
        }

        MetricsDto m = dashboard.metrics();
        prompt.append("Latest period: ").append(m.period()).append('\n');
        prompt.append("Revenue: ").append(m.revenue()).append('\n');
        prompt.append("Expenses: ").append(m.expenses()).append('\n');
        prompt.append("Profit: ").append(m.profit()).append('\n');
        prompt.append("Margin: ").append(m.margin()).append("%\n");
        if (m.runwayMonths() != null) {
            prompt.append("Runway: ").append(m.runwayMonths()).append(" months\n");
        }
        if (dashboard.monthlyComparison() != null) {
            prompt.append("Month-over-month comparison is available.\n");
        }
        for (RiskDto risk : dashboard.risks()) {
            prompt.append("Risk: ").append(risk.risk()).append(" (").append(risk.severity()).append(") - ")
                    .append(risk.evidence()).append('\n');
        }

        if (context.forecast() != null && context.forecast().hasEnoughData()) {
            prompt.append("\nA ").append(context.forecast().horizonMonths())
                    .append("-month deterministic forecast (linear trend over history) is available.\n");
        }
        return prompt.toString();
    }
}
