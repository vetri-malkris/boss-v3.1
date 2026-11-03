package com.boss.financial.domain;

import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A point-in-time copy of a {@link MonthlyFinancialSnapshot}'s numbers, captured
 * immediately before a re-upload overwrites them. Historical financial facts must
 * never be silently overwritten - this is the trace that lets anyone see exactly
 * what a month's numbers were before a correction, who made it, and when.
 * {@code createdAt} (from {@code BaseEntity}) is the moment of the revision.
 */
@Entity
@Table(name = "monthly_snapshot_revisions")
public class SnapshotRevision extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "previous_revenue", nullable = false)
    private BigDecimal previousRevenue;

    @Column(name = "previous_expenses", nullable = false)
    private BigDecimal previousExpenses;

    @Column(name = "previous_profit", nullable = false)
    private BigDecimal previousProfit;

    @Column(name = "previous_margin", nullable = false)
    private BigDecimal previousMargin;

    @Column(name = "previous_cash_balance")
    private BigDecimal previousCashBalance;

    @Column(name = "previous_runway_months")
    private BigDecimal previousRunwayMonths;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "revised_by")
    private UUID revisedBy;

    protected SnapshotRevision() {
    }

    public SnapshotRevision(UUID snapshotId, String period, BigDecimal previousRevenue, BigDecimal previousExpenses,
                             BigDecimal previousProfit, BigDecimal previousMargin, BigDecimal previousCashBalance,
                             BigDecimal previousRunwayMonths, UUID revisedBy) {
        this.snapshotId = snapshotId;
        this.period = period;
        this.previousRevenue = previousRevenue;
        this.previousExpenses = previousExpenses;
        this.previousProfit = previousProfit;
        this.previousMargin = previousMargin;
        this.previousCashBalance = previousCashBalance;
        this.previousRunwayMonths = previousRunwayMonths;
        this.revisedBy = revisedBy;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public String getPeriod() {
        return period;
    }

    public BigDecimal getPreviousRevenue() {
        return previousRevenue;
    }

    public BigDecimal getPreviousExpenses() {
        return previousExpenses;
    }

    public BigDecimal getPreviousProfit() {
        return previousProfit;
    }

    public BigDecimal getPreviousMargin() {
        return previousMargin;
    }

    public BigDecimal getPreviousCashBalance() {
        return previousCashBalance;
    }

    public BigDecimal getPreviousRunwayMonths() {
        return previousRunwayMonths;
    }

    public UUID getRevisedBy() {
        return revisedBy;
    }
}
