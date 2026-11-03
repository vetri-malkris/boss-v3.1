package com.boss.ai.controller;

import com.boss.ai.dto.ChatRequest;
import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.orchestration.AiOrchestrator;
import com.boss.subscription.service.EntitlementService;
import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI")
public class AiController {

    private final AiOrchestrator aiOrchestrator;
    private final EntitlementService entitlementService;

    public AiController(AiOrchestrator aiOrchestrator, EntitlementService entitlementService) {
        this.aiOrchestrator = aiOrchestrator;
        this.entitlementService = entitlementService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask BOSS a question about the caller's business data")
    public ApiResponse<ChatResponseDto> chat(@Valid @RequestBody ChatRequest request) {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        entitlementService.consume(1, "AI_ANALYSIS", "AI business analysis");
        return ApiResponse.of(aiOrchestrator.chat(tenantId, request.message()), MDC.get(LogFields.CORRELATION_ID));
    }
}
