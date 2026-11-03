package com.boss.dataentry.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.access.Subcategory;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.InvalidFileException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.domain.SubcategorySchema;
import com.boss.dataentry.dto.BulkUploadResultDto;
import com.boss.dataentry.dto.DataRecordDto;
import com.boss.dataentry.parser.TabularFileReader;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import com.boss.dataentry.schema.SubcategorySchemaRegistry;
import com.boss.dataentry.template.ExcelTemplateWriter;
import com.boss.dataentry.validation.FieldValidationException;
import com.boss.dataentry.validation.SchemaValidator;
import com.boss.financial.service.FinanceSnapshotSyncService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Backs every subcategory's data-entry form and bulk upload with one shared
 * implementation: validate a row against that subcategory's schema, persist it as a
 * {@link SubcategoryDataRecord}, tenant-scoped. Distinct per-subcategory field
 * definitions live in {@link SubcategorySchemaRegistry} - this service only enforces
 * "does the row match its schema", it never invents or computes a value itself.
 */
@Service
public class SubcategoryDataEntryService {

    private final SubcategorySchemaRegistry schemaRegistry;
    private final SchemaValidator schemaValidator;
    private final TabularFileReader tabularFileReader;
    private final SubcategoryDataRecordRepository repository;
    private final AuditService auditService;
    private final ExcelTemplateWriter templateWriter;
    private final FinanceSnapshotSyncService financeSnapshotSyncService;

    public SubcategoryDataEntryService(SubcategorySchemaRegistry schemaRegistry, SchemaValidator schemaValidator,
                                        TabularFileReader tabularFileReader, SubcategoryDataRecordRepository repository,
                                        AuditService auditService, ExcelTemplateWriter templateWriter,
                                        FinanceSnapshotSyncService financeSnapshotSyncService) {
        this.schemaRegistry = schemaRegistry;
        this.schemaValidator = schemaValidator;
        this.tabularFileReader = tabularFileReader;
        this.repository = repository;
        this.auditService = auditService;
        this.templateWriter = templateWriter;
        this.financeSnapshotSyncService = financeSnapshotSyncService;
    }

    /** A single-sheet template for just this subcategory - the same header/style
     * conventions as the category-wide template, scoped to one subcategory. */
    public Workbook buildTemplate(Subcategory subcategory) {
        SubcategorySchema schema = schemaRegistry.require(subcategory);
        Workbook workbook = new XSSFWorkbook();
        String sheetName = subcategory.label();
        templateWriter.writeHeaderSheet(workbook, sheetName, schema.fields());
        templateWriter.writeFieldGuide(workbook, List.of(new ExcelTemplateWriter.SheetSpec(sheetName, schema.fields())));
        return workbook;
    }

    public List<DataRecordDto> list(UUID tenantId, Subcategory subcategory) {
        return repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, subcategory).stream()
                .map(DataRecordDto::from)
                .toList();
    }

    @Transactional
    public DataRecordDto create(UUID tenantId, UUID userId, Subcategory subcategory, Map<String, String> rawValues) {
        SubcategorySchema schema = schemaRegistry.require(subcategory);
        Map<String, Object> validated;
        try {
            validated = schemaValidator.validate(schema, rawValues);
        } catch (FieldValidationException e) {
            throw new BusinessValidationException(e.getMessage());
        }

        SubcategoryDataRecord record = new SubcategoryDataRecord(subcategory, validated, userId);
        record.setTenantId(tenantId);
        record = repository.save(record);
        financeSnapshotSyncService.sync(userId, record);

        auditService.record(AuditEventType.DATA_UPLOAD, "SubcategoryDataRecord", record.getId().toString(),
                "New " + subcategory.label() + " record entered");
        return DataRecordDto.from(record);
    }

    /**
     * Corrects an existing record. The prior field values are preserved in the audit
     * trail (not discarded) so the correction itself is traceable, matching the same
     * "never silently overwrite" principle {@code SnapshotService} follows for
     * financial data.
     */
    @Transactional
    public DataRecordDto update(UUID tenantId, UUID userId, Subcategory subcategory, UUID recordId,
                                 Map<String, String> rawValues) {
        SubcategoryDataRecord record = ownedRecord(tenantId, subcategory, recordId);
        SubcategorySchema schema = schemaRegistry.require(subcategory);
        Map<String, Object> validated;
        try {
            validated = schemaValidator.validate(schema, rawValues);
        } catch (FieldValidationException e) {
            throw new BusinessValidationException(e.getMessage());
        }

        String diff = describeChanges(record.getFields(), validated);
        record.setFields(validated);
        record = repository.save(record);
        financeSnapshotSyncService.sync(userId, record);

        auditService.record(tenantId, userId, AuditEventType.RECORD_UPDATED, "SubcategoryDataRecord",
                record.getId().toString(),
                subcategory.label() + " record corrected" + (diff.isEmpty() ? "" : ": " + diff));
        return DataRecordDto.from(record);
    }

    /**
     * Voids (soft-deletes) a record. It disappears from the everyday list but is
     * never hard-deleted - deletion must be conservative, and a voided record stays
     * queryable/auditable via its id.
     */
    @Transactional
    public void voidRecord(UUID tenantId, UUID userId, Subcategory subcategory, UUID recordId, String reason) {
        SubcategoryDataRecord record = ownedRecord(tenantId, subcategory, recordId);
        record.voidRecord(userId, reason);
        repository.save(record);

        auditService.record(tenantId, userId, AuditEventType.RECORD_VOIDED, "SubcategoryDataRecord",
                record.getId().toString(), subcategory.label() + " record voided: " + reason);
    }

    private SubcategoryDataRecord ownedRecord(UUID tenantId, Subcategory subcategory, UUID recordId) {
        SubcategoryDataRecord record = repository.findById(recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("SubcategoryDataRecord", recordId));
        if (!record.getTenantId().equals(tenantId)) {
            throw TenantAccessDeniedException.forResource("SubcategoryDataRecord", recordId);
        }
        if (record.getSubcategory() != subcategory) {
            throw ResourceNotFoundException.of("SubcategoryDataRecord", recordId);
        }
        if (record.isVoided()) {
            throw new BusinessValidationException("This record has already been voided and can no longer be edited");
        }
        return record;
    }

    private String describeChanges(Map<String, Object> before, Map<String, Object> after) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());

        List<String> changes = new ArrayList<>();
        for (String key : keys) {
            Object oldValue = before.get(key);
            Object newValue = after.get(key);
            if (!java.util.Objects.equals(oldValue, newValue)) {
                changes.add(key + " '" + oldValue + "' -> '" + newValue + "'");
            }
        }
        return String.join(", ", changes);
    }

    @Transactional
    public BulkUploadResultDto bulkUpload(UUID tenantId, UUID userId, Subcategory subcategory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please choose a file to upload");
        }
        SubcategorySchema schema = schemaRegistry.require(subcategory);
        String extension = extensionOf(file.getOriginalFilename());

        List<Map<String, String>> rows;
        try {
            rows = tabularFileReader.read(file.getInputStream(), extension);
        } catch (IOException e) {
            throw new InvalidFileException("Could not read '" + file.getOriginalFilename() + "': " + e.getMessage());
        }

        List<String> warnings = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (templateWriter.isUnmodifiedExampleRow(schema.fields(), rows.get(i))) {
                continue;
            }
            long lineNumber = i + 2; // +1 for header, +1 for 1-based
            try {
                Map<String, Object> validated = schemaValidator.validate(schema, rows.get(i));
                SubcategoryDataRecord record = new SubcategoryDataRecord(subcategory, validated, userId);
                record.setTenantId(tenantId);
                record = repository.save(record);
                financeSnapshotSyncService.sync(userId, record);
                created++;
            } catch (FieldValidationException e) {
                warnings.add("Row " + lineNumber + ": " + e.getMessage());
            }
        }

        auditService.record(AuditEventType.DATA_UPLOAD, "SubcategoryDataRecord", null,
                "Bulk upload '" + file.getOriginalFilename() + "' into " + subcategory.label() + ": "
                        + created + " row(s) created, " + warnings.size() + " skipped");

        return new BulkUploadResultDto(file.getOriginalFilename(), created, warnings.size(), warnings);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("The file must have an extension (.csv or .xlsx)");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
