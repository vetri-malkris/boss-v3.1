package com.boss.ai.orchestration;

import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.forecasting.dto.ForecastResponseDto;

/**
 * Everything the LLM layer (real or mock) needs to ground an answer: the detected
 * intent plus the tenant's full dashboard state and (for forecast questions) a
 * forecast, gathered by the orchestrator via real tool calls (currently
 * {@code DashboardService}/{@code ForecastService}) - never by the LLM touching the
 * database directly (section 14). {@code forecast} is null unless the intent needed it.
 */
public record EvidenceContext(String userMessage, Intent intent, DashboardResponseDto dashboard,
                               ForecastResponseDto forecast) {
}
