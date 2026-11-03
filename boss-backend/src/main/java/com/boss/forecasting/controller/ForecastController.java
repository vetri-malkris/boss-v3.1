package com.boss.forecasting.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.forecasting.dto.ForecastResponseDto;
import com.boss.forecasting.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forecasts")
@Tag(name = "Forecasts")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    @Operation(summary = "Deterministic revenue/expense/cash/runway/break-even forecast for the caller's tenant")
    public ApiResponse<ForecastResponseDto> forecast(@RequestParam(defaultValue = "12") int horizonMonths) {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(forecastService.forecast(tenantId, horizonMonths), MDC.get(LogFields.CORRELATION_ID));
    }
}
