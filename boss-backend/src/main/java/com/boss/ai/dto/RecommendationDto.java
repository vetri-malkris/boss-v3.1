package com.boss.ai.dto;

import java.util.List;

public record RecommendationDto(
        String action,
        String expectedOutcome,
        double confidence,
        List<String> assumptions,
        List<String> alternatives,
        List<String> risks
) {
}
