package com.boss.ai.dto;

import java.util.List;

public record ChatResponseDto(
        String summary,
        List<EvidenceItemDto> evidence,
        DiagnosisDto diagnosis,
        List<RecommendationDto> recommendations
) {
}
