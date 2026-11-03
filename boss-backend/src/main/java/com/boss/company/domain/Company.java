package com.boss.company.domain;

import com.boss.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A legal business entity within a tenant. A tenant may operate more than one company
 * (e.g. group of subsidiaries); every financial/ingestion record downstream is scoped
 * to a companyId in addition to the tenantId.
 */
@Entity
@Table(name = "companies")
public class Company extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "fiscal_year_start_month", nullable = false)
    private int fiscalYearStartMonth = 4;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Company() {
    }

    public Company(String name, String currency, int fiscalYearStartMonth) {
        this.name = name;
        this.currency = currency;
        this.fiscalYearStartMonth = fiscalYearStartMonth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public int getFiscalYearStartMonth() {
        return fiscalYearStartMonth;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
