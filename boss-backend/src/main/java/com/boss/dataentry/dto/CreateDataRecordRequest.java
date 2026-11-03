package com.boss.dataentry.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateDataRecordRequest(@NotNull Map<String, String> values) {
}
