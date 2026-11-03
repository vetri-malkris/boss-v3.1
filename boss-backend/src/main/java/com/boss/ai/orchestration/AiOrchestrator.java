package com.boss.ai.orchestration;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.llm.AiClient;
import com.boss.ai.prompt.PromptBuilder;
import com.boss.ai.validator.ResponseValidator;
import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.service.DashboardService;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.service.ForecastService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The only place a chat message flows from the browser to "business tools" to an
 * {@link AiClient} and back (section 13). Evidence gathering ("tool calling") is
 * {@link DashboardService#build} plus, for forward-looking questions,
 * {@link ForecastService#forecast} - both already compose the financial/analytics/
 * risk engines, so a real multi-tool loop is a natural extension once questions need
 * more than "everything about this tenant right now (and its forecast)".
 */
@Service
public class AiOrchestrator {

    private static final int FORECAST_HORIZON_MONTHS = 12;

    private final IntentDetector intentDetector;
    private final DashboardService dashboardService;
    private final ForecastService forecastService;
    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final ResponseValidator responseValidator;
    private final AuditService auditService;

    public AiOrchestrator(IntentDetector intentDetector, DashboardService dashboardService,
                           ForecastService forecastService, PromptBuilder promptBuilder, AiClient aiClient,
                           ResponseValidator responseValidator, AuditService auditService) {
        this.intentDetector = intentDetector;
        this.dashboardService = dashboardService;
        this.forecastService = forecastService;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.responseValidator = responseValidator;
        this.auditService = auditService;
    }

    public ChatResponseDto chat(UUID tenantId, String message) {
        Intent intent = intentDetector.detect(message);
        DashboardResponseDto dashboard = dashboardService.build(tenantId);
        ForecastResponseDto forecast = intent == Intent.FORECAST
                ? forecastService.forecast(tenantId, FORECAST_HORIZON_MONTHS)
                : null;
        EvidenceContext context = new EvidenceContext(message, intent, dashboard, forecast);

        String prompt = promptBuilder.build(context);
        ChatResponseDto response = responseValidator.validate(aiClient.complete(prompt, context));

        // The question text itself is never logged/audited - only that a query happened
        // and what it was about (section 22/23: never log confidential AI prompts).
        auditService.record(AuditEventType.AI_QUERY, "AiChat", null, "AI chat query, intent=" + intent);

        return response;
    }
}
