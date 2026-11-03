package com.boss.financial.repository;

import com.boss.financial.domain.MonthlyFinancialSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyFinancialSnapshotRepository extends JpaRepository<MonthlyFinancialSnapshot, UUID> {

    Optional<MonthlyFinancialSnapshot> findByTenantIdAndPeriod(UUID tenantId, String period);

    List<MonthlyFinancialSnapshot> findByTenantIdOrderByPeriodAsc(UUID tenantId);
}
