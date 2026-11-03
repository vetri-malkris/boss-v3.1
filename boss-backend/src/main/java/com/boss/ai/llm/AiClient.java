package com.boss.ai.llm;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.orchestration.EvidenceContext;

/**
 * Abstraction over "whatever answers the question" - a real LLM provider
 * ({@code OpenAiClient}, not yet implemented) or {@link MockAiClient}. The
 * orchestrator is the only caller and never depends on a concrete implementation,
 * so swapping providers touches nothing else in the system (section 13).
 */
public interface AiClient {

    ChatResponseDto complete(String prompt, EvidenceContext context);
}
