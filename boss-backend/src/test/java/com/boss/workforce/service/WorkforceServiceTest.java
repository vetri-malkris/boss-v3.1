package com.boss.workforce.service;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceServiceTest {

    @Mock
    private SubcategoryDataRecordRepository repository;

    private WorkforceService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkforceService(repository);
    }

    @Test
    void monthlyPeopleCostSumsGrossSalaryBenefitsAndPayrollTaxAcrossDepartmentsForTheLatestPeriod() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST))
                .thenReturn(List.of(
                        record("2026-04", "Engineering", "400000", "40000", "20000"),
                        record("2026-04", "Sales", "180000", "15000", "10000")));

        assertThat(service.monthlyPeopleCost(tenantId)).isEqualByComparingTo("665000");
    }

    @Test
    void onlyTheLatestParsablePeriodIsSummedNotEveryHistoricalRecord() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST))
                .thenReturn(List.of(
                        record("2026-01", "Engineering", "350000", "30000", "15000"),
                        record("2026-04", "Engineering", "400000", "40000", "20000")));

        assertThat(service.monthlyPeopleCost(tenantId)).isEqualByComparingTo("460000");
    }

    @Test
    void optionalBenefitsAndPayrollTaxDefaultToZeroWhenAbsent() {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.PEOPLE_COST,
                Map.of("period", "2026-04", "department", "Engineering", "grossSalaryCost", new BigDecimal("400000")),
                userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST))
                .thenReturn(List.of(record));

        assertThat(service.monthlyPeopleCost(tenantId)).isEqualByComparingTo("400000");
    }

    @Test
    void unparsablePeriodIsSkippedNotGuessedAt() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST))
                .thenReturn(List.of(record("Q1 2026", "Engineering", "400000", "40000", "20000")));

        assertThat(service.monthlyPeopleCost(tenantId)).isNull();
    }

    @Test
    void noRecordsMeansNull() {
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.PEOPLE_COST))
                .thenReturn(List.of());

        assertThat(service.monthlyPeopleCost(tenantId)).isNull();
    }

    private SubcategoryDataRecord record(String period, String department, String grossSalaryCost,
                                          String benefitsCost, String payrollTaxCost) {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.PEOPLE_COST, Map.of(
                "period", period, "department", department, "grossSalaryCost", new BigDecimal(grossSalaryCost),
                "benefitsCost", new BigDecimal(benefitsCost), "payrollTaxCost", new BigDecimal(payrollTaxCost)), userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        return record;
    }
}
