package com.boss.tenant.controller;

import com.boss.common.response.ApiResponse;
import com.boss.common.logging.LogFields;
import com.boss.tenant.dto.CreateTenantRequest;
import com.boss.tenant.dto.TenantDto;
import com.boss.tenant.service.TenantService;
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
 * Platform-level tenant management. Restricted to SUPER_ADMIN - individual tenants
 * are provisioned either by the platform operator or via the public self-signup flow
 * in {@code AuthController}.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<ApiResponse<TenantDto>> create(@Valid @RequestBody CreateTenantRequest request) {
        TenantDto dto = TenantService.toDto(tenantService.createTenant(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(dto, MDC.get(LogFields.CORRELATION_ID)));
    }

    @GetMapping
    @Operation(summary = "List all tenants")
    public ApiResponse<List<TenantDto>> list() {
        List<TenantDto> tenants = tenantService.listAll().stream().map(TenantService::toDto).toList();
        return ApiResponse.of(tenants, MDC.get(LogFields.CORRELATION_ID));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tenant by id")
    public ApiResponse<TenantDto> getById(@PathVariable UUID id) {
        return ApiResponse.of(TenantService.toDto(tenantService.getById(id)), MDC.get(LogFields.CORRELATION_ID));
    }
}
