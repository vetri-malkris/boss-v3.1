package com.boss.identity.domain;

import com.boss.common.access.Subcategory;
import com.boss.common.domain.BaseEntity;
import com.boss.common.security.RoleName;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * A platform user. Email is globally unique. {@code tenantId} is {@code null} only for
 * SUPER_ADMIN accounts, which operate across tenants; every other role always belongs to
 * exactly one tenant in this phase of the product.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<RoleName> roles = EnumSet.noneOf(RoleName.class);

    /**
     * Which sidebar subcategories this user can see (section: dynamic per-user menu).
     * A category is considered "enabled" in the UI purely because at least one of its
     * subcategories is present here - there is no separate category-level flag, so an
     * "enabled category with zero subcategories" is not a representable state.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_subcategories", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "subcategory", nullable = false, length = 32)
    private Set<Subcategory> enabledSubcategories = EnumSet.noneOf(Subcategory.class);

    protected User() {
    }

    public User(UUID tenantId, String email, String passwordHash, String fullName, Set<RoleName> roles) {
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roles = roles;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }

    public Set<Subcategory> getEnabledSubcategories() {
        return enabledSubcategories;
    }

    public void setEnabledSubcategories(Set<Subcategory> enabledSubcategories) {
        this.enabledSubcategories = enabledSubcategories;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
