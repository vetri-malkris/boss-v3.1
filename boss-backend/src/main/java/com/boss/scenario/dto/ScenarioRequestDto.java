package com.boss.scenario.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * The variables a user can change in the scenario simulator (section 12). Percentages
 * are applied to every projected future month, not just the next one - a -10% revenue
 * scenario means "10% lower than trend for the whole horizon", not a one-off dip.
 */
public record ScenarioRequestDto(
        @NotNull @DecimalMin("-100") @DecimalMax("1000") BigDecimal revenueChangePercent,
        @NotNull @DecimalMin("-100") @DecimalMax("1000") BigDecimal expenseChangePercent,
        @NotNull @Min(0) Integer additionalHires,
        @NotNull @DecimalMin("0") BigDecimal avgMonthlyCostPerHire
) {
}
