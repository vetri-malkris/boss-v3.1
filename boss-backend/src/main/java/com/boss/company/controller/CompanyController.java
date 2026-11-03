package com.boss.company.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.company.dto.CompanyDto;
import com.boss.company.dto.CreateCompanyRequest;
import com.boss.company.service.CompanyService;
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

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Create a company under the caller's tenant")
    public ResponseEntity<ApiResponse<CompanyDto>> create(@Valid @RequestBody CreateCompanyRequest request) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        CompanyDto dto = CompanyService.toDto(companyService.create(tenantId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(dto, MDC.get(LogFields.CORRELATION_ID)));
    }

    @GetMapping
    @Operation(summary = "List companies under the caller's tenant")
    public ApiResponse<List<CompanyDto>> list() {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        List<CompanyDto> companies = companyService.listByTenant(tenantId).stream().map(CompanyService::toDto).toList();
        return ApiResponse.of(companies, MDC.get(LogFields.CORRELATION_ID));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company under the caller's tenant")
    public ApiResponse<CompanyDto> getById(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(CompanyService.toDto(companyService.getByIdForTenant(tenantId, id)), MDC.get(LogFields.CORRELATION_ID));
    }
}