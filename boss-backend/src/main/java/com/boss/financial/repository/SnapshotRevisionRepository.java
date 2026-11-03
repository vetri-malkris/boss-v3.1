package com.boss.financial.repository;

import com.boss.financial.domain.SnapshotRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SnapshotRevisionRepository extends JpaRepository<SnapshotRevision, UUID> {

    List<SnapshotRevision> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<SnapshotRevision> findByTenantIdAndPeriodOrderByCreatedAtDesc(UUID tenantId, String period);
}
