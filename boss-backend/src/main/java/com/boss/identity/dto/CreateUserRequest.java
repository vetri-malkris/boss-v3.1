package com.boss.identity.dto;

import com.boss.common.access.Subcategory;
import com.boss.common.security.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        @NotEmpty Set<RoleName> roles,
        @NotNull Set<Subcategory> enabledSubcategories
) {
}
