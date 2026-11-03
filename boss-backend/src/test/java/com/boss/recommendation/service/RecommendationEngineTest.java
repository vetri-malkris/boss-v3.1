package com.boss.recommendation.service;

import com.boss.analytics.service.TrendDetectionEngine;
import com.boss.finance.gst.service.GstService;
import com.boss.finance.kpi.FinanceKpiService;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.service.SnapshotService;
import com.boss.recommendation.domain.Recommendation;
import com.boss.recommendation.repository.RecommendationRepository;
import com.boss.risk.service.RiskEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineTest {

    @Mock
    private SnapshotService snapshotService;
    @Mock
    private RecommendationRepository repository;
    @Mock
    private GstService gstService;
    @Mock
    private FinanceKpiService financeKpiService;

    private RecommendationEngine engine;
    private final UUID tenantId = UUID.randomUUID();
    private static final FinanceKpiDto EMPTY_FINANCE_KPIS =
            new FinanceKpiDto(null, null, null, null, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        lenient().when(financeKpiService.receivables(tenantId)).thenReturn(null);
        lenient().when(financeKpiService.payables(tenantId)).thenReturn(null);
        lenient().when(gstService.currentLiability(tenantId)).thenReturn(null);
        lenient().when(financeKpiService.summarize(tenantId)).thenReturn(EMPTY_FINANCE_KPIS);
        lenient().when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        engine = new RecommendationEngine(snapshotService, new TrendDetectionEngine(), new RiskEngine(), repository,
                gstService, financeKpiService);
    }

    @Test
    void returnsNoRecommendationsWithoutAnyHistoryOrFinanceKpiRisks() {
        when(snapshotService.history(tenantId)).thenReturn(List.of());

        List<Recommendation> result = engine.generate(tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    void generatesFinanceKpiRecommendationsEvenWithoutAnyTransactionHistory() {
        when(snapshotService.history(tenantId)).thenReturn(List.of());
        when(financeKpiService.summarize(tenantId)).thenReturn(
                new FinanceKpiDto(null, null, new BigDecimal("0.5"), null, null, null, null, null, null, null));

        List<Recommendation> result = engine.generate(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIssue()).isEqualTo("Low current ratio (Financial Position)");
    }

    @Test
    void healthyBusinessGetsNoRecommendations() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(snapshot("2026-01", "10000", "5000")));

        List<Recommendation> result = engine.generate(tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    void aLossGeneratesAGroundedRecommendation() {
        when(snapshotService.history(tenantId)).thenReturn(List.of(snapshot("2026-01", "1000", "1500")));

        List<Recommendation> result = engine.generate(tenantId);

        assertThat(result).hasSize(1);
        Recommendation recommendation = result.get(0);
        assertThat(recommendation.getIssue()).isEqualTo("Operating at a loss");
        assertThat(recommendation.getRootCause()).isEqualTo("Expenses exceeded revenue this period.");
        assertThat(recommendation.getConfidence()).isEqualByComparingTo("0.55");
        assertThat(recommendation.getAlternatives()).isNotEmpty();
        assertThat(recommendation.getEvidence()).isNotEmpty();
        assertThat(recommendation.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void persistsOneRecommendationPerDetectedRisk() {
        // Loss + implicitly short runway if cash were known; here just verify the
        // repository receives exactly the risks the engine detected, not more or fewer.
        MonthlyFinancialSnapshot snapshot = snapshot("2026-01", "1000", "1500");
        snapshot.applyCashPosition(new BigDecimal("2000"), new BigDecimal("2.0"));
        when(snapshotService.history(tenantId)).thenReturn(List.of(snapshot));

        ArgumentCaptor<List<Recommendation>> captor = ArgumentCaptor.forClass(List.class);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        engine.generate(tenantId);

        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .extracting(Recommendation::getIssue)
                .containsExactlyInAnyOrder("Operating at a loss", "Short cash runway");
    }

    private MonthlyFinancialSnapshot snapshot(String period, String revenue, String expenses) {
        BigDecimal rev = new BigDecimal(revenue);
        BigDecimal exp = new BigDecimal(expenses);
        return new MonthlyFinancialSnapshot(period, rev, exp, rev.subtract(exp), BigDecimal.ZERO);
    }
}
