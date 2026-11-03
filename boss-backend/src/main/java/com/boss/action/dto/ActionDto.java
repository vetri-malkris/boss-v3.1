package com.boss.action.dto;

import com.boss.action.domain.ActionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActionDto(
        UUID id,
        UUID recommendationId,
        String recommendationIssue,
        String description,
        ActionStatus status,
        String proposedByEmail,
        Instant createdAt,
        String approvedByEmail,
        Instant approvedAt,
        Instant executedAt,
        Instant completedAt,
        String expectedOutcomeMetric,
        BigDecimal expectedOutcomeValue,
        BigDecimal actualOutcomeValue,
        BigDecimal variance,
        String outcomeReason
) {
}
