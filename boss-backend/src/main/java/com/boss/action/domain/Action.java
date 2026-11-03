package com.boss.action.domain;

import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * A human-approved (or rejected) action taken on a {@code Recommendation} (section
 * 16/17). High-impact actions are never auto-executed - every state transition here
 * requires an explicit authenticated call, gated by role at the controller
 * (see {@code ActionController}). Expected/actual outcome values are always supplied
 * by a person; {@code variance} is the only value this class computes itself.
 */
@Entity
@Table(name = "actions")
public class Action extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recommendation_id", nullable = false, updatable = false)
    private UUID recommendationId;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActionStatus status = ActionStatus.PENDING_APPROVAL;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "proposed_by", nullable = false, updatable = false)
    private UUID proposedBy;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expected_outcome_metric", nullable = false)
    private String expectedOutcomeMetric;

    @Column(name = "expected_outcome_value", nullable = false)
    private BigDecimal expectedOutcomeValue;

    @Column(name = "actual_outcome_value")
    private BigDecimal actualOutcomeValue;

    @Column(name = "variance")
    private BigDecimal variance;

    @Column(name = "outcome_reason", length = 1000)
    private String outcomeReason;

    protected Action() {
    }

    public Action(UUID recommendationId, String description, UUID proposedBy, String expectedOutcomeMetric,
                  BigDecimal expectedOutcomeValue) {
        this.recommendationId = recommendationId;
        this.description = description;
        this.proposedBy = proposedBy;
        this.expectedOutcomeMetric = expectedOutcomeMetric;
        this.expectedOutcomeValue = expectedOutcomeValue;
    }

    public void approve(UUID approverId) {
        requireStatus(ActionStatus.PENDING_APPROVAL, "approve");
        this.status = ActionStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
    }

    public void reject(UUID approverId) {
        requireStatus(ActionStatus.PENDING_APPROVAL, "reject");
        this.status = ActionStatus.REJECTED;
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
    }

    public void start() {
        requireStatus(ActionStatus.APPROVED, "start");
        this.status = ActionStatus.IN_PROGRESS;
        this.executedAt = Instant.now();
    }

    public void complete(BigDecimal actualOutcomeValue, String outcomeReason) {
        if (status != ActionStatus.APPROVED && status != ActionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete an action in status " + status);
        }
        this.status = ActionStatus.COMPLETED;
        this.actualOutcomeValue = actualOutcomeValue;
        this.variance = actualOutcomeValue.subtract(expectedOutcomeValue).setScale(2, RoundingMode.HALF_UP);
        this.outcomeReason = outcomeReason;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (status == ActionStatus.COMPLETED || status == ActionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an action in status " + status);
        }
        this.status = ActionStatus.CANCELLED;
    }

    private void requireStatus(ActionStatus required, String operation) {
        if (status != required) {
            throw new IllegalStateException("Cannot " + operation + " an action in status " + status + " (expected " + required + ")");
        }
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public String getDescription() {
        return description;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public UUID getProposedBy() {
        return proposedBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getExpectedOutcomeMetric() {
        return expectedOutcomeMetric;
    }

    public BigDecimal getExpectedOutcomeValue() {
        return expectedOutcomeValue;
    }

    public BigDecimal getActualOutcomeValue() {
        return actualOutcomeValue;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }
}
