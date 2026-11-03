package com.boss.tenant.service;

import com.boss.audit.service.AuditService;
import com.boss.common.exception.DuplicateResourceException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.tenant.domain.Tenant;
import com.boss.tenant.dto.CreateTenantRequest;
import com.boss.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private AuditService auditService;

    private TenantService tenantService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, auditService);
    }

    @Test
    void createsTenantWhenSlugIsAvailable() {
        when(tenantRepository.existsBySlugIgnoreCase("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Tenant tenant = tenantService.createTenant(new CreateTenantRequest("Acme Inc", "acme"));

        assertThat(tenant.getName()).isEqualTo("Acme Inc");
        assertThat(tenant.getSlug()).isEqualTo("acme");
    }

    @Test
    void rejectsDuplicateSlug() {
        when(tenantRepository.existsBySlugIgnoreCase("acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(new CreateTenantRequest("Acme Inc", "acme")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getByIdThrowsWhenTenantDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(tenantRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getById(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
