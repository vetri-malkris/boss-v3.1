package com.boss.recommendation.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.recommendation.domain.Recommendation;
import com.boss.recommendation.dto.EvidenceItemDto;
import com.boss.recommendation.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Thin façade: generation (via {@link RecommendationEngine}) + entity-to-DTO mapping + audit. */
@Service
public class RecommendationService {

    private final RecommendationEngine engine;
    private final AuditService auditService;

    public RecommendationService(RecommendationEngine engine, AuditService auditService) {
        this.engine = engine;
        this.auditService = auditService;
    }

    public List<RecommendationDto> generate(UUID tenantId) {
        List<Recommendation> created = engine.generate(tenantId);
        for (Recommendation recommendation : created) {
            auditService.record(AuditEventType.RECOMMENDATION_CREATED, "Recommendation",
                    recommendation.getId().toString(), "Recommendation created: " + recommendation.getIssue());
        }
        return created.stream().map(RecommendationService::toDto).toList();
    }

    public List<RecommendationDto> list(UUID tenantId) {
        return engine.list(tenantId).stream().map(RecommendationService::toDto).toList();
    }

    public static RecommendationDto toDto(Recommendation r) {
        return new RecommendationDto(
                r.getId(), r.getPeriod(), r.getIssue(),
                r.getEvidence().stream().map(e -> new EvidenceItemDto(e.getLabel(), e.getValue())).toList(),
                r.getRootCause(), r.getImpact(), r.getRecommendedAction(), r.getAlternatives(), r.getExpectedOutcome(),
                r.getRisks(), r.getConfidence(), r.getAssumptions(), r.getTimeHorizon(), r.getFollowUpMeasurement(),
                r.getCreatedAt());
    }
}
