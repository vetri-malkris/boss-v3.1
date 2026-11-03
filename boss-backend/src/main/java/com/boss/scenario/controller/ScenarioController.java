package com.boss.scenario.controller;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.scenario.dto.ScenarioRequestDto;
import com.boss.scenario.dto.ScenarioResultDto;
import com.boss.scenario.service.ScenarioEngine;
import com.boss.subscription.service.EntitlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scenarios")
@Tag(name = "Scenarios")
public class ScenarioController {

    private final ScenarioEngine scenarioEngine;
    private final AuditService auditService;
    private final EntitlementService entitlementService;

    public ScenarioController(ScenarioEngine scenarioEngine, AuditService auditService, EntitlementService entitlementService) {
        this.scenarioEngine = scenarioEngine;
        this.auditService = auditService;
        this.entitlementService = entitlementService;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate a revenue/expense/hiring scenario against the caller's forecast")
    public ApiResponse<ScenarioResultDto> simulate(@Valid @RequestBody ScenarioRequestDto request,
                                                     @RequestParam(defaultValue = "12") int horizonMonths) {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        entitlementService.consume(1, "SCENARIO_SIMULATION", "Scenario simulation");
        ScenarioResultDto result = scenarioEngine.simulate(tenantId, request, horizonMonths);
        auditService.record(AuditEventType.SCENARIO_RUN, "Scenario", null,
                "Scenario run: revenue " + request.revenueChangePercent() + "%, expense " + request.expenseChangePercent()
                        + "%, +" + request.additionalHires() + " hire(s)");
        return ApiResponse.of(result, MDC.get(LogFields.CORRELATION_ID));
    }
}
