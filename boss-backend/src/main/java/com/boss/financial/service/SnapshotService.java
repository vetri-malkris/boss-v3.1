package com.boss.financial.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.financial.calculator.MarginCalculator;
import com.boss.financial.calculator.RunwayCalculator;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.domain.SnapshotRevision;
import com.boss.financial.dto.SnapshotRevisionDto;
import com.boss.financial.repository.MonthlyFinancialSnapshotRepository;
import com.boss.financial.repository.SnapshotRevisionRepository;
import com.boss.workforce.service.WorkforceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Upserts one {@link MonthlyFinancialSnapshot} per calendar month, derived from the
 * Finance subcategory forms (Profitability for revenue/expenses, Position for cash,
 * People Cost for payroll burn) - see {@code FinanceSnapshotSyncService}, the only
 * caller of the upsert/recompute methods below. This is the only place those
 * per-period aggregates survive past a single request (section 6/22).
 *
 * Historical facts must never be silently overwritten: if a correction changes an
 * *existing* month's numbers, the prior values are captured as a
 * {@link SnapshotRevision} before being overwritten, so a correction is always
 * traceable (who, when, what it used to say) rather than simply lost.
 */
@Service
public class SnapshotService {

    private final MonthlyFinancialSnapshotRepository repository;
    private final SnapshotRevisionRepository revisionRepository;
    private final MarginCalculator marginCalculator;
    private final RunwayCalculator runwayCalculator;
    private final WorkforceService workforceService;
    private final AuditService auditService;

    public SnapshotService(MonthlyFinancialSnapshotRepository repository, SnapshotRevisionRepository revisionRepository,
                            MarginCalculator marginCalculator, RunwayCalculator runwayCalculator,
                            WorkforceService workforceService, AuditService auditService) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.marginCalculator = marginCalculator;
        this.runwayCalculator = runwayCalculator;
        this.workforceService = workforceService;
        this.auditService = auditService;
    }

    /** Called whenever a Profitability record is saved for a given period. */
    @Transactional
    public void upsertPeriodFinancials(UUID tenantId, UUID userId, String period, BigDecimal revenue, BigDecimal expenses) {
        BigDecimal profit = revenue.subtract(expenses);
        BigDecimal margin = marginCalculator.calculate(profit, revenue);

        var existing = repository.findByTenantIdAndPeriod(tenantId, period);
        MonthlyFinancialSnapshot snapshot = existing.orElseGet(() -> {
            MonthlyFinancialSnapshot created = new MonthlyFinancialSnapshot(period, revenue, expenses, profit, margin);
            created.setTenantId(tenantId);
            return created;
        });

        if (existing.isPresent() && hasChanged(snapshot, revenue, expenses, profit, margin)) {
            recordRevision(tenantId, snapshot, userId);
        }

        snapshot.update(revenue, expenses, profit, margin);
        recomputeRunway(tenantId, snapshot);
        repository.save(snapshot);
    }

    /** Called whenever a Position record is saved for a given period (the month of
     * its as-of date). */
    @Transactional
    public void upsertCashPosition(UUID tenantId, UUID userId, String period, BigDecimal cashBalance) {
        var existing = repository.findByTenantIdAndPeriod(tenantId, period);
        MonthlyFinancialSnapshot snapshot = existing.orElseGet(() -> {
            MonthlyFinancialSnapshot created = new MonthlyFinancialSnapshot(
                    period, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            created.setTenantId(tenantId);
            return created;
        });

        boolean cashChanged = existing.isPresent() && !cashBalance.equals(snapshot.getCashBalance());
        if (cashChanged) {
            recordRevision(tenantId, snapshot, userId);
        }

        snapshot.applyCashPosition(cashBalance, null);
        recomputeRunway(tenantId, snapshot);
        repository.save(snapshot);
    }

    /**
     * Re-derives the latest period's runway from whatever is currently the most
     * up-to-date People Cost total - called whenever a People Cost record is saved,
     * since that's a third independent input to the same burn figure and nothing
     * else would otherwise trigger a refresh of an already-stored runway.
     */
    @Transactional
    public void recomputeLatestRunway(UUID tenantId) {
        List<MonthlyFinancialSnapshot> history = history(tenantId);
        if (history.isEmpty()) {
            return;
        }
        MonthlyFinancialSnapshot latest = history.get(history.size() - 1);
        recomputeRunway(tenantId, latest);
        repository.save(latest);
    }

    /**
     * Runway (months) = cash balance / normalized monthly burn, where burn = this
     * period's (expenses - revenue) from Profitability PLUS the tenant's current
     * People Cost - included here (not inside Profitability's own expenses) so it's
     * counted exactly once regardless of whether a user's Operating Expenses entry
     * happens to already include payroll; this is the one place that assumption is
     * made, and it's made explicitly rather than silently. Left {@code null} (never
     * fabricated, never NaN/Infinity) when there's no cash balance yet or burn is
     * zero/negative - {@link RunwayCalculator} already encodes that rule.
     */
    private void recomputeRunway(UUID tenantId, MonthlyFinancialSnapshot snapshot) {
        if (snapshot.getCashBalance() == null) {
            return;
        }
        BigDecimal peopleCost = workforceService.monthlyPeopleCost(tenantId);
        BigDecimal burn = snapshot.getExpenses().subtract(snapshot.getRevenue())
                .add(peopleCost == null ? BigDecimal.ZERO : peopleCost);
        BigDecimal runwayMonths = runwayCalculator.calculate(snapshot.getCashBalance(), burn);
        snapshot.applyCashPosition(snapshot.getCashBalance(), runwayMonths);
    }

    private boolean hasChanged(MonthlyFinancialSnapshot snapshot, BigDecimal revenue, BigDecimal expenses,
                                BigDecimal profit, BigDecimal margin) {
        return snapshot.getRevenue().compareTo(revenue) != 0
                || snapshot.getExpenses().compareTo(expenses) != 0
                || snapshot.getProfit().compareTo(profit) != 0
                || snapshot.getMargin().compareTo(margin) != 0;
    }

    private void recordRevision(UUID tenantId, MonthlyFinancialSnapshot snapshot, UUID uploadedBy) {
        SnapshotRevision revision = new SnapshotRevision(
                snapshot.getId(), snapshot.getPeriod(), snapshot.getRevenue(), snapshot.getExpenses(),
                snapshot.getProfit(), snapshot.getMargin(), snapshot.getCashBalance(), snapshot.getRunwayMonths(),
                uploadedBy);
        revision.setTenantId(tenantId);
        revisionRepository.save(revision);

        auditService.record(tenantId, uploadedBy, AuditEventType.SNAPSHOT_REVISED, "MonthlyFinancialSnapshot",
                snapshot.getId().toString(),
                "Re-upload revised " + snapshot.getPeriod() + ": revenue " + snapshot.getRevenue()
                        + ", expenses " + snapshot.getExpenses() + ", profit " + snapshot.getProfit()
                        + " were the prior values before this correction");
    }

    @Transactional(readOnly = true)
    public List<MonthlyFinancialSnapshot> history(UUID tenantId) {
        return repository.findByTenantIdOrderByPeriodAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<SnapshotRevisionDto> revisions(UUID tenantId) {
        return revisionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(r -> new SnapshotRevisionDto(r.getPeriod(), r.getPreviousRevenue(), r.getPreviousExpenses(),
                        r.getPreviousProfit(), r.getPreviousMargin(), r.getCreatedAt()))
                .toList();
    }
}
