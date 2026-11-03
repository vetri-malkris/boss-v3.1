package com.boss.financial.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.financial.calculator.MarginCalculator;
import com.boss.financial.calculator.RunwayCalculator;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.domain.SnapshotRevision;
import com.boss.financial.repository.MonthlyFinancialSnapshotRepository;
import com.boss.financial.repository.SnapshotRevisionRepository;
import com.boss.workforce.service.WorkforceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private MonthlyFinancialSnapshotRepository repository;
    @Mock
    private SnapshotRevisionRepository revisionRepository;
    @Mock
    private WorkforceService workforceService;
    @Mock
    private AuditService auditService;

    private SnapshotService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(workforceService.monthlyPeopleCost(tenantId)).thenReturn(null);
        service = new SnapshotService(repository, revisionRepository, new MarginCalculator(),
                new RunwayCalculator(), workforceService, auditService);
    }

    @Test
    void upsertPeriodFinancialsCreatesANewSnapshotWhenNoneExists() {
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertPeriodFinancials(tenantId, userId, "2026-01", new BigDecimal("1000.00"), new BigDecimal("400.00"));

        ArgumentCaptor<MonthlyFinancialSnapshot> captor = ArgumentCaptor.forClass(MonthlyFinancialSnapshot.class);
        verify(repository).save(captor.capture());
        MonthlyFinancialSnapshot saved = captor.getValue();
        assertThat(saved.getPeriod()).isEqualTo("2026-01");
        assertThat(saved.getRevenue()).isEqualByComparingTo("1000.00");
        assertThat(saved.getExpenses()).isEqualByComparingTo("400.00");
        assertThat(saved.getProfit()).isEqualByComparingTo("600.00");

        // A brand-new period is not a "correction" - nothing to revise.
        verifyNoInteractions(revisionRepository, auditService);
    }

    @Test
    void upsertPeriodFinancialsUpdatesAnExistingSnapshotInsteadOfCreatingADuplicate() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("900.00"),
                new BigDecimal("300.00"), new BigDecimal("600.00"), new BigDecimal("66.67"));
        existing.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertPeriodFinancials(tenantId, userId, "2026-01", new BigDecimal("1500.00"), new BigDecimal("400.00"));

        verify(repository).save(existing);
        assertThat(existing.getRevenue()).isEqualByComparingTo("1500.00");
    }

    @Test
    void upsertPeriodFinancialsRecordsARevisionWhenAnExistingPeriodChanges() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("900.00"),
                new BigDecimal("300.00"), new BigDecimal("600.00"), new BigDecimal("66.67"));
        existing.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertPeriodFinancials(tenantId, userId, "2026-01", new BigDecimal("1500.00"), new BigDecimal("400.00"));

        ArgumentCaptor<SnapshotRevision> captor = ArgumentCaptor.forClass(SnapshotRevision.class);
        verify(revisionRepository).save(captor.capture());
        SnapshotRevision revision = captor.getValue();
        assertThat(revision.getPeriod()).isEqualTo("2026-01");
        assertThat(revision.getPreviousRevenue()).isEqualByComparingTo("900.00"); // the OLD value, not the new one
        assertThat(revision.getRevisedBy()).isEqualTo(userId);

        verify(auditService).record(eq(tenantId), eq(userId), eq(AuditEventType.SNAPSHOT_REVISED),
                anyString(), anyString(), anyString());
    }

    @Test
    void upsertPeriodFinancialsWithIdenticalNumbersDoesNotRecordASpuriousRevision() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("900.00"),
                new BigDecimal("300.00"), new BigDecimal("600.00"), new BigDecimal("66.67"));
        existing.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertPeriodFinancials(tenantId, userId, "2026-01", new BigDecimal("900.00"), new BigDecimal("300.00"));

        verify(revisionRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void upsertCashPositionCreatesAZeroRevenuePlaceholderWhenNoSnapshotExistsYet() {
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertCashPosition(tenantId, userId, "2026-01", new BigDecimal("5000"));

        ArgumentCaptor<MonthlyFinancialSnapshot> captor = ArgumentCaptor.forClass(MonthlyFinancialSnapshot.class);
        verify(repository).save(captor.capture());
        MonthlyFinancialSnapshot saved = captor.getValue();
        assertThat(saved.getCashBalance()).isEqualByComparingTo("5000");
        assertThat(saved.getRevenue()).isEqualByComparingTo("0");
        // No burn rate known yet (zero revenue/expenses placeholder) - runway is "not
        // applicable", never fabricated.
        assertThat(saved.getRunwayMonths()).isNull();
        verifyNoInteractions(revisionRepository, auditService);
    }

    @Test
    void upsertCashPositionComputesRunwayWhenAMatchingPeriodAlreadyHasFinancials() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("1000.00"),
                new BigDecimal("1500.00"), new BigDecimal("-500.00"), new BigDecimal("-50.00"));
        existing.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertCashPosition(tenantId, userId, "2026-01", new BigDecimal("5000"));

        // burn = expenses(1500) - revenue(1000) = 500/month; runway = 5000 / 500 = 10.0
        assertThat(existing.getRunwayMonths()).isEqualByComparingTo("10.0");
    }

    @Test
    void upsertCashPositionRecordsARevisionWhenCashBalanceChangesForAnExistingPeriod() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("900.00"),
                new BigDecimal("300.00"), new BigDecimal("600.00"), new BigDecimal("66.67"));
        existing.setId(UUID.randomUUID());
        existing.applyCashPosition(new BigDecimal("1000"), null);
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertCashPosition(tenantId, userId, "2026-01", new BigDecimal("2000"));

        verify(revisionRepository).save(any());
        verify(auditService).record(eq(tenantId), eq(userId), eq(AuditEventType.SNAPSHOT_REVISED),
                anyString(), anyString(), anyString());
    }

    @Test
    void upsertCashPositionIncludesPeopleCostInBurnExactlyOnce() {
        // Matches the acceptance scenario: revenue 100000, expenses 400000 (before
        // payroll), people cost 100000 -> burn = (400000-100000)+100000 = 400000;
        // cash 1200000 / 400000 = 3.0 months runway.
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-04", new BigDecimal("100000"),
                new BigDecimal("400000"), new BigDecimal("-300000"), new BigDecimal("-300.00"));
        existing.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-04")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workforceService.monthlyPeopleCost(tenantId)).thenReturn(new BigDecimal("100000"));

        service.upsertCashPosition(tenantId, userId, "2026-04", new BigDecimal("1200000"));

        assertThat(existing.getRunwayMonths()).isEqualByComparingTo("3.0");
    }

    @Test
    void upsertPeriodFinancialsIncludesPeopleCostInBurnWhenACashBalanceAlreadyExists() {
        MonthlyFinancialSnapshot existing = new MonthlyFinancialSnapshot("2026-04", new BigDecimal("900.00"),
                new BigDecimal("300.00"), new BigDecimal("600.00"), new BigDecimal("66.67"));
        existing.setId(UUID.randomUUID());
        existing.applyCashPosition(new BigDecimal("1200000"), null);
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-04")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workforceService.monthlyPeopleCost(tenantId)).thenReturn(new BigDecimal("100000"));

        service.upsertPeriodFinancials(tenantId, userId, "2026-04", new BigDecimal("100000"), new BigDecimal("400000"));

        assertThat(existing.getRunwayMonths()).isEqualByComparingTo("3.0");
    }

    @Test
    void upsertPeriodFinancialsLeavesRunwayNullWhenNoCashBalanceEnteredYet() {
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertPeriodFinancials(tenantId, userId, "2026-01", new BigDecimal("100000"), new BigDecimal("400000"));

        verify(workforceService, never()).monthlyPeopleCost(any());
    }

    @Test
    void recomputeLatestRunwayRefreshesTheMostRecentPeriodWhenPeopleCostChanges() {
        MonthlyFinancialSnapshot january = new MonthlyFinancialSnapshot("2026-01", new BigDecimal("50000"),
                new BigDecimal("30000"), new BigDecimal("20000"), new BigDecimal("40.00"));
        MonthlyFinancialSnapshot april = new MonthlyFinancialSnapshot("2026-04", new BigDecimal("100000"),
                new BigDecimal("400000"), new BigDecimal("-300000"), new BigDecimal("-300.00"));
        april.applyCashPosition(new BigDecimal("1200000"), null);
        when(repository.findByTenantIdOrderByPeriodAsc(tenantId)).thenReturn(List.of(january, april));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workforceService.monthlyPeopleCost(tenantId)).thenReturn(new BigDecimal("100000"));

        service.recomputeLatestRunway(tenantId);

        assertThat(april.getRunwayMonths()).isEqualByComparingTo("3.0");
        verify(repository).save(april);
        verify(repository, never()).save(january);
    }

    @Test
    void recomputeLatestRunwayIsANoOpWhenThereIsNoHistoryYet() {
        when(repository.findByTenantIdOrderByPeriodAsc(tenantId)).thenReturn(List.of());

        service.recomputeLatestRunway(tenantId);

        verify(repository, never()).save(any());
    }
}
