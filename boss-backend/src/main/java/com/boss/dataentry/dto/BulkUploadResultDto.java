package com.boss.dataentry.dto;

import java.util.List;

public record BulkUploadResultDto(String fileName, int rowsCreated, int rowsSkipped, List<String> warnings) {
}
