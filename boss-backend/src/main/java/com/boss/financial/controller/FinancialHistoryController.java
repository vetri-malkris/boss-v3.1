package com.boss.financial.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.SecurityUtils;
import com.boss.financial.dto.SnapshotRevisionDto;
import com.boss.financial.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Surfaces the correction trail for monthly financial snapshots - historical facts
 * are never silently overwritten (see {@code SnapshotService}), and that trail must
 * be visible, not just logged, for it to count as real traceability.
 */
@RestController
@RequestMapping("/api/v1/financial")
@Tag(name = "Financial history")
public class FinancialHistoryController {

    private final SnapshotService snapshotService;

    public FinancialHistoryController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/revisions")
    @Operation(summary = "Every correction made to a monthly snapshot by a later re-upload, most recent first")
    public ApiResponse<List<SnapshotRevisionDto>> revisions() {
        UUID tenantId = SecurityUtils.requireCurrentUser().getTenantId();
        return ApiResponse.of(snapshotService.revisions(tenantId), MDC.get(LogFields.CORRELATION_ID));
    }
}
