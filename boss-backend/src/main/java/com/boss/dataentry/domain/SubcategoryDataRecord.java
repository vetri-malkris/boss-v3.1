package com.boss.dataentry.domain;

import com.boss.common.access.Subcategory;
import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One entered row of data for a subcategory (e.g. one Hiring requisition, one
 * Tax/GST filing) - real user-entered data, tagged by subcategory and validated
 * against that subcategory's {@link SubcategorySchema} before being persisted.
 * Field values live in a jsonb column because the columns differ per subcategory;
 * the schema (and therefore the actual shape of the data) is still explicit,
 * distinct, per-subcategory domain metadata, not a generic label/value system.
 *
 * Corrections are conservative (section: data integrity): a record can be edited
 * (the prior values are captured in the audit trail by the service layer, not
 * discarded) or voided, but is never hard-deleted - a voided record stays
 * queryable/auditable, it just drops out of the everyday list.
 */
@Entity
@Table(name = "subcategory_data_records")
public class SubcategoryDataRecord extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "subcategory", nullable = false, length = 32)
    private Subcategory subcategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> fields = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "voided", nullable = false)
    private boolean voided = false;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    protected SubcategoryDataRecord() {
    }

    public SubcategoryDataRecord(Subcategory subcategory, Map<String, Object> fields, UUID createdBy) {
        this.subcategory = subcategory;
        this.fields = new LinkedHashMap<>(fields);
        this.createdBy = createdBy;
    }

    public Subcategory getSubcategory() {
        return subcategory;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public boolean isVoided() {
        return voided;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public UUID getVoidedBy() {
        return voidedBy;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void voidRecord(UUID voidedBy, String reason) {
        this.voided = true;
        this.voidedAt = Instant.now();
        this.voidedBy = voidedBy;
        this.voidReason = reason;
    }
}
