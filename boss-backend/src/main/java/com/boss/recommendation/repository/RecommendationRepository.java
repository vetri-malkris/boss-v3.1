package com.boss.recommendation.repository;

import com.boss.recommendation.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
