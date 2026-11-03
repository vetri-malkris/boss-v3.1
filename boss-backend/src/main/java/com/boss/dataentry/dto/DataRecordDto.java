package com.boss.dataentry.dto;

import com.boss.dataentry.domain.SubcategoryDataRecord;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DataRecordDto(UUID id, String subcategory, Map<String, Object> fields, Instant createdAt,
                             Instant updatedAt) {

    public static DataRecordDto from(SubcategoryDataRecord record) {
        return new DataRecordDto(record.getId(), record.getSubcategory().name(), record.getFields(),
                record.getCreatedAt(), record.getUpdatedAt());
    }
}
