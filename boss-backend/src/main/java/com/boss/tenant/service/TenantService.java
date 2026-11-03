package com.boss.tenant.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.exception.DuplicateResourceException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.tenant.domain.Tenant;
import com.boss.tenant.dto.CreateTenantRequest;
import com.boss.tenant.dto.TenantDto;
import com.boss.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new DuplicateResourceException("A tenant with slug '" + request.slug() + "' already exists");
        }
        Tenant tenant = tenantRepository.save(new Tenant(request.name(), request.slug()));
        auditService.record(tenant.getId(), null, AuditEventType.TENANT_CREATED, "Tenant", tenant.getId().toString(),
                "Tenant '" + tenant.getName() + "' created");
        return tenant;
    }

    @Transactional(readOnly = true)
    public Tenant getById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
    }

    @Transactional(readOnly = true)
    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    public static TenantDto toDto(Tenant tenant) {
        return new TenantDto(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getStatus(),
                tenant.getDataRetentionDays(), tenant.getCreatedAt());
    }
}
