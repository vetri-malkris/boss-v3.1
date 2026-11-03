package com.boss.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Security principal representing the caller resolved from a validated JWT.
 * Deliberately decoupled from the {@code identity} module's JPA entity so that
 * {@code common} never depends on a feature module.
 */
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final Set<RoleName> roles;
    private final boolean enabled;

    public AuthenticatedUser(UUID userId, UUID tenantId, String email, Set<RoleName> roles, boolean enabled) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
