package com.boss.action.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompleteActionRequest(
        @NotNull BigDecimal actualOutcomeValue,
        @Size(max = 1000) String outcomeReason
) {
}
