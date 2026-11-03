package com.boss.identity.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.access.Subcategory;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.DuplicateResourceException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.common.security.RoleName;
import com.boss.identity.domain.User;
import com.boss.identity.dto.CreateUserRequest;
import com.boss.identity.dto.UserSummaryDto;
import com.boss.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the user lifecycle. Every lookup that is scoped to a tenant enforces tenant
 * isolation explicitly rather than relying on a global Hibernate filter, so the
 * enforcement is visible and testable at the service boundary (section 4).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /** Used for the tenant self-signup bootstrap admin, who gets every subcategory. */
    @Transactional
    public User createUser(UUID tenantId, String email, String rawPassword, String fullName, Set<RoleName> roles) {
        return createUser(tenantId, email, rawPassword, fullName, roles, EnumSet.allOf(Subcategory.class));
    }

    @Transactional
    public User createUser(UUID tenantId, String email, String rawPassword, String fullName, Set<RoleName> roles,
                            Set<Subcategory> enabledSubcategories) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists");
        }
        User user = new User(tenantId, email.toLowerCase(), passwordEncoder.encode(rawPassword), fullName, roles);
        user.setEnabledSubcategories(enabledSubcategories);
        user = userRepository.save(user);
        auditService.record(tenantId, user.getId(), AuditEventType.USER_CREATED, "User", user.getId().toString(),
                "User '" + user.getEmail() + "' created with roles " + roles);
        return user;
    }

    /** Tenant-scoped creation only (invite/create via {@code UserController}) - never
     * lets a tenant admin or CEO mint a SUPER_ADMIN account through this path.
     * SUPER_ADMIN accounts are platform-level (null tenantId) and only exist via the
     * seeded bootstrap account or another SUPER_ADMIN acting through a dedicated flow. */
    @Transactional
    public User createUser(UUID tenantId, CreateUserRequest request) {
        if (request.roles().contains(RoleName.SUPER_ADMIN)) {
            throw new BusinessValidationException("SUPER_ADMIN cannot be assigned from tenant-scoped user creation");
        }
        return createUser(tenantId, request.email(), request.password(), request.fullName(), request.roles(),
                request.enabledSubcategories());
    }

    @Transactional(readOnly = true)
    public User getByIdForTenant(UUID tenantId, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        assertBelongsToTenant(user, tenantId);
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> listByTenant(UUID tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void deactivate(UUID tenantId, UUID userId) {
        User user = getByIdForTenant(tenantId, userId);
        user.setActive(false);
        auditService.record(tenantId, userId, AuditEventType.SETTINGS_CHANGED, "User", userId.toString(),
                "User '" + user.getEmail() + "' deactivated");
    }

    @Transactional
    public User updateAccess(UUID tenantId, UUID userId, Set<Subcategory> enabledSubcategories) {
        User user = getByIdForTenant(tenantId, userId);
        user.setEnabledSubcategories(enabledSubcategories);
        auditService.record(tenantId, userId, AuditEventType.SETTINGS_CHANGED, "User", userId.toString(),
                "User '" + user.getEmail() + "' menu access updated");
        return user;
    }

    private void assertBelongsToTenant(User user, UUID tenantId) {
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw TenantAccessDeniedException.forResource("User", user.getId());
        }
    }

    public static UserSummaryDto toDto(User user) {
        return new UserSummaryDto(user.getId(), user.getEmail(), user.getFullName(), user.getTenantId(),
                user.getRoles(), user.isActive(), user.getEnabledSubcategories());
    }
}
