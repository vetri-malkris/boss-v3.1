package com.boss.dataentry.repository;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubcategoryDataRecordRepository extends JpaRepository<SubcategoryDataRecord, UUID> {

    List<SubcategoryDataRecord> findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(
            UUID tenantId, Subcategory subcategory);
}
