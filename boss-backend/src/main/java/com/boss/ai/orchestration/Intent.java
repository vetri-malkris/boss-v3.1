package com.boss.ai.orchestration;

/**
 * A coarse classification of what the user is asking about. Real intent detection
 * (or letting an LLM choose which tools to call) is a natural upgrade path once a
 * real {@code AiClient} is wired in - this keyword-based version is what
 * {@link com.boss.ai.llm.MockAiClient} needs to ground its answer in real data.
 */
public enum Intent {
    PROFIT,
    REVENUE,
    EXPENSES,
    MARGIN,
    RUNWAY,
    RISKS,
    FORECAST,
    GENERAL
}
