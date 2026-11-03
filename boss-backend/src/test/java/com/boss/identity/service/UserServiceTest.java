package com.boss.identity.service;

import com.boss.audit.service.AuditService;
import com.boss.common.access.Subcategory;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.common.security.RoleName;
import com.boss.identity.domain.User;
import com.boss.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private UserService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, auditService);
    }

    private void stubSave() {
        when(userRepository.save(any())).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
    }

    @Test
    void bootstrapAdminSignupGetsEveryLastSubcategory() {
        stubSave();
        User admin = service.createUser(tenantId, "admin@acme.test", "password123", "Admin", Set.of(RoleName.TENANT_ADMIN));

        assertThat(admin.getEnabledSubcategories()).containsExactlyInAnyOrderElementsOf(Set.of(Subcategory.values()));
    }

    @Test
    void invitedUserGetsExactlyTheSubcategoriesRequested() {
        stubSave();
        Set<Subcategory> requested = EnumSet.of(Subcategory.POSITION, Subcategory.LIQUIDITY);

        User user = service.createUser(tenantId, "viewer@acme.test", "password123", "Viewer",
                Set.of(RoleName.VIEWER), requested);

        assertThat(user.getEnabledSubcategories()).containsExactlyInAnyOrderElementsOf(requested);
    }

    @Test
    void canCreateAUserWithNoAccessYet() {
        stubSave();
        User user = service.createUser(tenantId, "viewer@acme.test", "password123", "Viewer",
                Set.of(RoleName.VIEWER), Set.of());

        assertThat(user.getEnabledSubcategories()).isEmpty();
    }

    @Test
    void updateAccessReplacesThePreviousSelection() {
        User existing = new User(tenantId, "viewer@acme.test", "hash", "Viewer", Set.of(RoleName.VIEWER));
        existing.setId(UUID.randomUUID());
        existing.setEnabledSubcategories(EnumSet.of(Subcategory.POSITION));
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        User updated = service.updateAccess(tenantId, existing.getId(),
                EnumSet.of(Subcategory.RISK_FINANCIAL, Subcategory.RISK_MITIGATION));

        assertThat(updated.getEnabledSubcategories())
                .containsExactlyInAnyOrder(Subcategory.RISK_FINANCIAL, Subcategory.RISK_MITIGATION);
    }

    @Test
    void cannotUpdateAccessForAUserInAnotherTenant() {
        User existing = new User(UUID.randomUUID(), "viewer@acme.test", "hash", "Viewer", Set.of(RoleName.VIEWER));
        existing.setId(UUID.randomUUID());
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateAccess(tenantId, existing.getId(), Set.of(Subcategory.POSITION)))
                .isInstanceOf(TenantAccessDeniedException.class);
    }
}
