package com.boss.action.service;

import com.boss.action.domain.Action;
import com.boss.action.dto.ActionDto;
import com.boss.action.dto.CompleteActionRequest;
import com.boss.action.dto.ProposeActionRequest;
import com.boss.action.repository.ActionRepository;
import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.identity.repository.UserRepository;
import com.boss.recommendation.domain.Recommendation;
import com.boss.recommendation.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The approval workflow (section 16/17): propose -> approve/reject -> start ->
 * complete (records actual outcome, computes variance) or cancel. Every transition
 * is an explicit authenticated call - nothing here runs on its own.
 */
@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ActionService(ActionRepository actionRepository, RecommendationRepository recommendationRepository,
                          UserRepository userRepository, AuditService auditService) {
        this.actionRepository = actionRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActionDto propose(UUID tenantId, UUID userId, ProposeActionRequest request) {
        Recommendation recommendation = recommendationRepository.findById(request.recommendationId())
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> ResourceNotFoundException.of("Recommendation", request.recommendationId()));

        Action action = new Action(recommendation.getId(), request.description(), userId,
                request.expectedOutcomeMetric(), request.expectedOutcomeValue());
        action.setTenantId(tenantId);
        action = actionRepository.save(action);

        auditService.record(AuditEventType.ACTION_CREATED, "Action", action.getId().toString(),
                "Action proposed from recommendation '" + recommendation.getIssue() + "'");
        return toDto(action);
    }

    @Transactional
    public ActionDto approve(UUID tenantId, UUID approverId, UUID actionId) {
        Action action = get(tenantId, actionId);
        transition(() -> action.approve(approverId), "approve");
        actionRepository.save(action);
        auditService.record(AuditEventType.ACTION_APPROVED, "Action", action.getId().toString(), "Action approved");
        return toDto(action);
    }

    @Transactional
    public ActionDto reject(UUID tenantId, UUID approverId, UUID actionId) {
        Action action = get(tenantId, actionId);
        transition(() -> action.reject(approverId), "reject");
        actionRepository.save(action);
        auditService.record(AuditEventType.ACTION_REJECTED, "Action", action.getId().toString(), "Action rejected");
        return toDto(action);
    }

    @Transactional
    public ActionDto start(UUID tenantId, UUID actionId) {
        Action action = get(tenantId, actionId);
        transition(action::start, "start");
        actionRepository.save(action);
        auditService.record(AuditEventType.ACTION_EXECUTED, "Action", action.getId().toString(), "Action started");
        return toDto(action);
    }

    @Transactional
    public ActionDto complete(UUID tenantId, UUID actionId, CompleteActionRequest request) {
        Action action = get(tenantId, actionId);
        transition(() -> action.complete(request.actualOutcomeValue(), request.outcomeReason()), "complete");
        actionRepository.save(action);
        auditService.record(AuditEventType.ACTION_COMPLETED, "Action", action.getId().toString(),
                "Action completed, variance " + action.getVariance());
        auditService.record(AuditEventType.OUTCOME_RECORDED, "Action", action.getId().toString(),
                "Outcome recorded for action");
        return toDto(action);
    }

    @Transactional
    public ActionDto cancel(UUID tenantId, UUID actionId) {
        Action action = get(tenantId, actionId);
        transition(action::cancel, "cancel");
        actionRepository.save(action);
        auditService.record(AuditEventType.ACTION_CANCELLED, "Action", action.getId().toString(), "Action cancelled");
        return toDto(action);
    }

    @Transactional(readOnly = true)
    public List<ActionDto> list(UUID tenantId) {
        return actionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDto).toList();
    }

    private Action get(UUID tenantId, UUID actionId) {
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Action", actionId));
        if (!action.getTenantId().equals(tenantId)) {
            throw TenantAccessDeniedException.forResource("Action", actionId);
        }
        return action;
    }

    private void transition(Runnable transition, String operation) {
        try {
            transition.run();
        } catch (IllegalStateException e) {
            throw new BusinessValidationException(e.getMessage());
        }
    }

    private ActionDto toDto(Action action) {
        String recommendationIssue = recommendationRepository.findById(action.getRecommendationId())
                .map(Recommendation::getIssue)
                .orElse("(recommendation not found)");
        String proposedByEmail = resolveEmail(action.getProposedBy());
        String approvedByEmail = action.getApprovedBy() != null ? resolveEmail(action.getApprovedBy()) : null;

        return new ActionDto(action.getId(), action.getRecommendationId(), recommendationIssue, action.getDescription(),
                action.getStatus(), proposedByEmail, action.getCreatedAt(), approvedByEmail, action.getApprovedAt(),
                action.getExecutedAt(), action.getCompletedAt(), action.getExpectedOutcomeMetric(),
                action.getExpectedOutcomeValue(), action.getActualOutcomeValue(), action.getVariance(),
                action.getOutcomeReason());
    }

    private String resolveEmail(UUID userId) {
        return userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
    }
}
