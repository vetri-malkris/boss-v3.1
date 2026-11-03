package com.boss.financial.domain;

import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One tenant's summarized business performance for one calendar month
 * (section 6). Populated from uploads via {@code SnapshotService}; never
 * stores raw transactions - only the aggregated numbers, per section 22.
 */
@Entity
@Table(name = "monthly_financial_snapshots")
public class MonthlyFinancialSnapshot extends TenantScopedEntity {

    @Column(name = "period", nullable = false, length = 7)
    private String period; // "YYYY-MM"

    @Column(name = "revenue", nullable = false)
    private BigDecimal revenue;

    @Column(name = "expenses", nullable = false)
    private BigDecimal expenses;

    @Column(name = "profit", nullable = false)
    private BigDecimal profit;

    @Column(name = "margin", nullable = false)
    private BigDecimal margin;

    @Column(name = "cash_balance")
    private BigDecimal cashBalance;

    @Column(name = "runway_months")
    private BigDecimal runwayMonths;

    @Column(name = "receivables")
    private BigDecimal receivables;

    @Column(name = "payables")
    private BigDecimal payables;

    protected MonthlyFinancialSnapshot() {
    }

    public MonthlyFinancialSnapshot(String period, BigDecimal revenue, BigDecimal expenses, BigDecimal profit,
                                     BigDecimal margin) {
        this.period = period;
        this.revenue = revenue;
        this.expenses = expenses;
        this.profit = profit;
        this.margin = margin;
    }

    public String getPeriod() {
        return period;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public BigDecimal getMargin() {
        return margin;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public BigDecimal getRunwayMonths() {
        return runwayMonths;
    }

    public BigDecimal getReceivables() {
        return receivables;
    }

    public BigDecimal getPayables() {
        return payables;
    }

    public void update(BigDecimal revenue, BigDecimal expenses, BigDecimal profit, BigDecimal margin) {
        this.revenue = revenue;
        this.expenses = expenses;
        this.profit = profit;
        this.margin = margin;
    }

    public void applyCashPosition(BigDecimal cashBalance, BigDecimal runwayMonths) {
        this.cashBalance = cashBalance;
        this.runwayMonths = runwayMonths;
    }
}
