package com.boss.recommendation.domain;

import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A persisted, evidence-grounded recommendation (section 15). Generated deterministically
 * by {@code RecommendationEngine} from the risk engine's output - never by the LLM
 * inventing figures. Gets a stable id so a later phase's Action can reference it via
 * {@code recommendationId} (section 17).
 */
@Entity
@Table(name = "recommendations")
public class Recommendation extends TenantScopedEntity {

    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "source_risk")
    private String sourceRisk;

    @Column(name = "issue", nullable = false)
    private String issue;

    @Column(name = "root_cause", nullable = false, length = 1000)
    private String rootCause;

    @Column(name = "impact", nullable = false, length = 1000)
    private String impact;

    @Column(name = "recommended_action", nullable = false, length = 1000)
    private String recommendedAction;

    @Column(name = "expected_outcome", nullable = false, length = 1000)
    private String expectedOutcome;

    @Column(name = "confidence", nullable = false)
    private BigDecimal confidence;

    @Column(name = "time_horizon", nullable = false, length = 100)
    private String timeHorizon;

    @Column(name = "follow_up_measurement", nullable = false, length = 500)
    private String followUpMeasurement;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_evidence", joinColumns = @JoinColumn(name = "recommendation_id"))
    @OrderColumn(name = "position")
    private List<EvidenceEntry> evidence = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_alternatives", joinColumns = @JoinColumn(name = "recommendation_id"))
    @OrderColumn(name = "position")
    @Column(name = "alternative", length = 500)
    private List<String> alternatives = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_risks", joinColumns = @JoinColumn(name = "recommendation_id"))
    @OrderColumn(name = "position")
    @Column(name = "risk", length = 500)
    private List<String> risks = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_assumptions", joinColumns = @JoinColumn(name = "recommendation_id"))
    @OrderColumn(name = "position")
    @Column(name = "assumption", length = 500)
    private List<String> assumptions = new ArrayList<>();

    protected Recommendation() {
    }

    public Recommendation(String period, String sourceRisk, String issue, String rootCause, String impact,
                           String recommendedAction, String expectedOutcome, BigDecimal confidence,
                           String timeHorizon, String followUpMeasurement) {
        this.period = period;
        this.sourceRisk = sourceRisk;
        this.issue = issue;
        this.rootCause = rootCause;
        this.impact = impact;
        this.recommendedAction = recommendedAction;
        this.expectedOutcome = expectedOutcome;
        this.confidence = confidence;
        this.timeHorizon = timeHorizon;
        this.followUpMeasurement = followUpMeasurement;
    }

    public String getPeriod() {
        return period;
    }

    public String getSourceRisk() {
        return sourceRisk;
    }

    public String getIssue() {
        return issue;
    }

    public String getRootCause() {
        return rootCause;
    }

    public String getImpact() {
        return impact;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getTimeHorizon() {
        return timeHorizon;
    }

    public String getFollowUpMeasurement() {
        return followUpMeasurement;
    }

    public List<EvidenceEntry> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceEntry> evidence) {
        this.evidence = evidence;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<String> alternatives) {
        this.alternatives = alternatives;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }
}
