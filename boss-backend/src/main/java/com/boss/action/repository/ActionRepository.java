package com.boss.action.repository;

import com.boss.action.domain.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionRepository extends JpaRepository<Action, UUID> {

    List<Action> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Action> findByIdAndTenantId(UUID id, UUID tenantId);
}
