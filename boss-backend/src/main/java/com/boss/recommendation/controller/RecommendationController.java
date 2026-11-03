package com.boss.recommendation.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.recommendation.dto.RecommendationDto;
import com.boss.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "List previously generated recommendations for the caller's tenant")
    public ApiResponse<List<RecommendationDto>> list() {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(recommendationService.list(tenantId), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate fresh recommendations from the caller's current risks")
    public ApiResponse<List<RecommendationDto>> generate() {
        var tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(recommendationService.generate(tenantId), MDC.get(LogFields.CORRELATION_ID));
    }
}
