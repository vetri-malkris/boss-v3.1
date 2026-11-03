package com.boss.identity.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.identity.domain.User;
import com.boss.identity.dto.CreateUserRequest;
import com.boss.identity.dto.UpdateUserAccessRequest;
import com.boss.identity.dto.UserSummaryDto;
import com.boss.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User management, always scoped to the caller's own tenant - there is no endpoint here
 * that accepts a tenantId from the client (see section 4: tenant isolation).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','CEO','CFO','FINANCE_MANAGER','MANAGER')")
    @Operation(summary = "List users in the caller's tenant")
    public ApiResponse<List<UserSummaryDto>> list() {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        List<UserSummaryDto> users = userService.listByTenant(tenantId).stream().map(UserService::toDto).toList();
        return ApiResponse.of(users, MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','CEO')")
    @Operation(summary = "Invite/create a new user in the caller's tenant")
    public ResponseEntity<ApiResponse<UserSummaryDto>> create(@Valid @RequestBody CreateUserRequest request) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        User user = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(UserService.toDto(user), MDC.get(LogFields.CORRELATION_ID)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','CEO','CFO','FINANCE_MANAGER','MANAGER')")
    @Operation(summary = "Get a user in the caller's tenant")
    public ApiResponse<UserSummaryDto> getById(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(UserService.toDto(userService.getByIdForTenant(tenantId, id)), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','CEO')")
    @Operation(summary = "Deactivate a user in the caller's tenant")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        userService.deactivate(tenantId, id);
        return ApiResponse.of(null, MDC.get(LogFields.CORRELATION_ID));
    }

    @PatchMapping("/{id}/access")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','CEO')")
    @Operation(summary = "Change which sidebar subcategories a user can see")
    public ApiResponse<UserSummaryDto> updateAccess(@PathVariable UUID id, @Valid @RequestBody UpdateUserAccessRequest request) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        User user = userService.updateAccess(tenantId, id, request.enabledSubcategories());
        return ApiResponse.of(UserService.toDto(user), MDC.get(LogFields.CORRELATION_ID));
    }
}
