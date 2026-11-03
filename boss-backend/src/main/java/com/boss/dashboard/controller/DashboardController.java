package com.boss.dashboard.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.dashboard.dto.DashboardResponseDto;
import com.boss.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Load the caller's complete dashboard state in one round trip")
    public ApiResponse<DashboardResponseDto> getDashboard() {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(dashboardService.build(tenantId), MDC.get(LogFields.CORRELATION_ID));
    }
}
