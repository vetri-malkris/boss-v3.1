package com.boss.identity.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
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
 * Cross-tenant user management for SUPER_ADMIN only - the one place a tenantId is
 * trusted from the client, because the caller here is explicitly authorized to act on
 * any tenant (unlike {@link UserController}, which always scopes to the caller's own
 * tenant). Reuses the exact same {@link UserService} methods that controller calls, so
 * tenant-scoped user management has one implementation regardless of who triggers it.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
@Tag(name = "Users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantUserController {

    private final UserService userService;

    public TenantUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users in a given tenant (SUPER_ADMIN only)")
    public ApiResponse<List<UserSummaryDto>> list(@PathVariable UUID tenantId) {
        List<UserSummaryDto> users = userService.listByTenant(tenantId).stream().map(UserService::toDto).toList();
        return ApiResponse.of(users, MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping
    @Operation(summary = "Invite/create a new user in a given tenant (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<UserSummaryDto>> create(@PathVariable UUID tenantId,
                                                               @Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(UserService.toDto(user), MDC.get(LogFields.CORRELATION_ID)));
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate a user in a given tenant (SUPER_ADMIN only)")
    public ApiResponse<Void> deactivate(@PathVariable UUID tenantId, @PathVariable UUID userId) {
        userService.deactivate(tenantId, userId);
        return ApiResponse.of(null, MDC.get(LogFields.CORRELATION_ID));
    }

    @PatchMapping("/{userId}/access")
    @Operation(summary = "Change which sidebar subcategories a user can see (SUPER_ADMIN only)")
    public ApiResponse<UserSummaryDto> updateAccess(@PathVariable UUID tenantId, @PathVariable UUID userId,
                                                     @Valid @RequestBody UpdateUserAccessRequest request) {
        User user = userService.updateAccess(tenantId, userId, request.enabledSubcategories());
        return ApiResponse.of(UserService.toDto(user), MDC.get(LogFields.CORRELATION_ID));
    }
}
