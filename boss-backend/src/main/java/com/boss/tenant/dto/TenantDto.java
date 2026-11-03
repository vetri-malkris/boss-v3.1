package com.boss.tenant.dto;

import com.boss.tenant.domain.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        int dataRetentionDays,
        Instant createdAt
) {
}
