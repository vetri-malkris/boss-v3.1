package com.boss.dataentry.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidRecordRequest(@NotBlank String reason) {
}
