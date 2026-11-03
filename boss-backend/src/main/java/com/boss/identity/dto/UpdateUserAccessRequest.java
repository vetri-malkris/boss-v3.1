package com.boss.identity.dto;

import com.boss.common.access.Subcategory;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserAccessRequest(@NotNull Set<Subcategory> enabledSubcategories) {
}
