package com.boss.company.dto;

import java.util.UUID;

public record CompanyDto(
        UUID id,
        UUID tenantId,
        String name,
        String currency,
        int fiscalYearStartMonth,
        boolean active
) {
}
