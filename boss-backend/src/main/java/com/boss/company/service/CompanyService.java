package com.boss.company.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.company.domain.Company;
import com.boss.company.dto.CompanyDto;
import com.boss.company.dto.CreateCompanyRequest;
import com.boss.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AuditService auditService;

    public CompanyService(CompanyRepository companyRepository, AuditService auditService) {
        this.companyRepository = companyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Company create(UUID tenantId, CreateCompanyRequest request) {
        Company company = new Company(request.name(), request.currency(), request.fiscalYearStartMonth());
        company.setTenantId(tenantId);
        company = companyRepository.save(company);
        auditService.record(AuditEventType.COMPANY_CREATED, "Company", company.getId().toString(),
                "Company '" + company.getName() + "' created");
        return company;
    }

    @Transactional(readOnly = true)
    public List<Company> listByTenant(UUID tenantId) {
        return companyRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Company getByIdForTenant(UUID tenantId, UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
        if (!company.getTenantId().equals(tenantId)) {
            throw TenantAccessDeniedException.forResource("Company", companyId);
        }
        return company;
    }

    public static CompanyDto toDto(Company company) {
        return new CompanyDto(company.getId(), company.getTenantId(), company.getName(), company.getCurrency(),
                company.getFiscalYearStartMonth(), company.isActive());
    }
}
