package com.boss.recommendation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Mirrors the recommendation shape in section 15: evidence, diagnosis, recommended action, confidence, assumptions, alternatives, expected outcome, risks. */
public record RecommendationDto(
        UUID id,
        String period,
        String issue,
        List<EvidenceItemDto> evidence,
        String rootCause,
        String impact,
        String recommendedAction,
        List<String> alternatives,
        String expectedOutcome,
        List<String> risks,
        BigDecimal confidence,
        List<String> assumptions,
        String timeHorizon,
        String followUpMeasurement,
        Instant createdAt
) {
}
