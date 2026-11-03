package com.boss.action.controller;

import com.boss.action.dto.ActionDto;
import com.boss.action.dto.CompleteActionRequest;
import com.boss.action.dto.ProposeActionRequest;
import com.boss.action.service.ActionService;
import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.AuthenticatedUser;
import com.boss.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Proposing an action is open to any authenticated tenant user (it's just a
 * suggestion); approving/rejecting/completing requires an elevated role - the
 * human-approval gate from section 16.
 */
@RestController
@RequestMapping("/api/v1/actions")
@Tag(name = "Actions")
public class ActionController {

    private static final String APPROVER_ROLES = "hasAnyRole('TENANT_ADMIN','CFO','CEO')";

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping
    @Operation(summary = "List actions for the caller's tenant")
    public ApiResponse<List<ActionDto>> list() {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(actionService.list(tenantId), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping
    @Operation(summary = "Propose an action from a recommendation")
    public ResponseEntity<ApiResponse<ActionDto>> propose(@Valid @RequestBody ProposeActionRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        ActionDto dto = actionService.propose(user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(dto, MDC.get(LogFields.CORRELATION_ID)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(APPROVER_ROLES)
    @Operation(summary = "Approve a pending action")
    public ApiResponse<ActionDto> approve(@PathVariable UUID id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return ApiResponse.of(actionService.approve(user.getTenantId(), user.getUserId(), id), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(APPROVER_ROLES)
    @Operation(summary = "Reject a pending action")
    public ApiResponse<ActionDto> reject(@PathVariable UUID id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return ApiResponse.of(actionService.reject(user.getTenantId(), user.getUserId(), id), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(APPROVER_ROLES)
    @Operation(summary = "Mark an approved action as in progress")
    public ApiResponse<ActionDto> start(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(actionService.start(tenantId, id), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(APPROVER_ROLES)
    @Operation(summary = "Complete an action and record its actual outcome")
    public ApiResponse<ActionDto> complete(@PathVariable UUID id, @Valid @RequestBody CompleteActionRequest request) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(actionService.complete(tenantId, id, request), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an action")
    public ApiResponse<ActionDto> cancel(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(actionService.cancel(tenantId, id), MDC.get(LogFields.CORRELATION_ID));
    }
}
