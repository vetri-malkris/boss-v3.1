package com.boss.financial.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A correction record: what a month's numbers were before a re-upload overwrote them. */
public record SnapshotRevisionDto(
        String period,
        BigDecimal previousRevenue,
        BigDecimal previousExpenses,
        BigDecimal previousProfit,
        BigDecimal previousMargin,
        Instant revisedAt
) {
}
