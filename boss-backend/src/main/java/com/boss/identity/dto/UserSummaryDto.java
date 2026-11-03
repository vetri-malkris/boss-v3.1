package com.boss.identity.dto;

import com.boss.common.access.Subcategory;
import com.boss.common.security.RoleName;

import java.util.Set;
import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String email,
        String fullName,
        UUID tenantId,
        Set<RoleName> roles,
        boolean active,
        Set<Subcategory> enabledSubcategories
) {
}
