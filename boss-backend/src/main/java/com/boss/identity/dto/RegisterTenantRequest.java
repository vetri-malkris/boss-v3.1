package com.boss.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public self-signup: provisions a new tenant together with its first TENANT_ADMIN user.
 */
public record RegisterTenantRequest(
        @NotBlank @Size(max = 255) String tenantName,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
                message = "tenantSlug must be lowercase alphanumeric with hyphens") String tenantSlug,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8, max = 128) String adminPassword,
        @NotBlank @Size(max = 255) String adminFullName
) {
}
