package com.boss.dataentry.categoryupload;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.access.Category;
import com.boss.common.access.Subcategory;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.InvalidFileException;
import com.boss.dataentry.categoryupload.dto.CategoryUploadResultDto;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.domain.SubcategorySchema;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The category-level upload workflow (distinct from {@code SubcategoryDataEntryService}'s
 * per-subcategory bulk upload, which deliberately skips bad rows and keeps good ones).
 * Here, one workbook covers every uploadable subcategory in a category at once - one
 * sheet each - and the whole file is validated before anything is written: any single
 * bad row anywhere aborts the entire upload with nothing persisted, so a partially
 * wrong file can never leave partially wrong data behind.
 *
 * For Finance specifically, saving a Position or Profitability row also keeps
 * {@code MonthlyFinancialSnapshot} in sync via {@link FinanceSnapshotSyncService} -
 * see that class for the mapping. There is no separate raw-transaction upload path
 * anymore; those two subcategory forms are the single authoritative source.
 */
@Service
public class CategoryUploadService {

    private final SubcategorySchemaRegistry schemaRegistry;
    private final SchemaValidator schemaValidator;
    private final TabularFileReader tabularFileReader;
    private final SubcategoryDataRecordRepository repository;
    private final FinanceSnapshotSyncService financeSnapshotSyncService;
    private final AuditService auditService;
    private final ExcelTemplateWriter templateWriter;

    public CategoryUploadService(SubcategorySchemaRegistry schemaRegistry, SchemaValidator schemaValidator,
                                  TabularFileReader tabularFileReader, SubcategoryDataRecordRepository repository,
                                  FinanceSnapshotSyncService financeSnapshotSyncService,
                                  AuditService auditService, ExcelTemplateWriter templateWriter) {
        this.schemaRegistry = schemaRegistry;
        this.schemaValidator = schemaValidator;
        this.tabularFileReader = tabularFileReader;
        this.repository = repository;
        this.financeSnapshotSyncService = financeSnapshotSyncService;
        this.auditService = auditService;
        this.templateWriter = templateWriter;
    }

    public Workbook buildTemplate(Category category) {
        Workbook workbook = new XSSFWorkbook();
        List<ExcelTemplateWriter.SheetSpec> specs = new ArrayList<>();

        for (Subcategory sub : registeredSubcategoriesFor(category)) {
            SubcategorySchema schema = schemaRegistry.require(sub);
            templateWriter.writeHeaderSheet(workbook, sheetName(sub), schema.fields());
            specs.add(new ExcelTemplateWriter.SheetSpec(sheetName(sub), schema.fields()));
        }
        templateWriter.writeFieldGuide(workbook, specs);
        return workbook;
    }

    @Transactional
    public CategoryUploadResultDto bulkUpload(UUID tenantId, UUID userId, Category category, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please choose a file to upload");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!"xlsx".equalsIgnoreCase(extension) && !"xls".equalsIgnoreCase(extension)) {
            throw new InvalidFileException("Please upload the .xlsx template file for this category.");
        }

        Map<String, List<Map<String, String>>> sheets;
        try {
            sheets = tabularFileReader.readAllSheets(file.getInputStream());
        } catch (IOException e) {
            throw new InvalidFileException("Could not read '" + file.getOriginalFilename() + "': " + e.getMessage());
        }

        List<String> errors = new ArrayList<>();

        record PendingSheet(Subcategory subcategory, List<Map<String, Object>> rows) {
        }
        List<PendingSheet> pending = new ArrayList<>();
        for (Subcategory sub : registeredSubcategoriesFor(category)) {
            String name = sheetName(sub);
            List<Map<String, String>> rows = sheets.getOrDefault(name, List.of());
            SubcategorySchema schema = schemaRegistry.require(sub);
            List<Map<String, Object>> validatedRows = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (templateWriter.isUnmodifiedExampleRow(schema.fields(), rows.get(i))) {
                    continue;
                }
                try {
                    validatedRows.add(schemaValidator.validate(schema, rows.get(i)));
                } catch (FieldValidationException e) {
                    errors.add("Sheet '" + name + "', row " + (i + 2) + ": " + e.getMessage());
                }
            }
            pending.add(new PendingSheet(sub, validatedRows));
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(String.join("\n", errors));
        }

        Map<String, Integer> recordsBySheet = new LinkedHashMap<>();
        int total = 0;
        for (PendingSheet sheet : pending) {
            for (Map<String, Object> values : sheet.rows()) {
                SubcategoryDataRecord record = new SubcategoryDataRecord(sheet.subcategory(), values, userId);
                record.setTenantId(tenantId);
                record = repository.save(record);
                financeSnapshotSyncService.sync(userId, record);
            }
            if (!sheet.rows().isEmpty()) {
                recordsBySheet.put(sheetName(sheet.subcategory()), sheet.rows().size());
                total += sheet.rows().size();
            }
        }

        auditService.record(AuditEventType.DATA_UPLOAD, "CategoryUpload", null,
                "Bulk upload '" + file.getOriginalFilename() + "' into " + category.label() + ": " + total + " record(s) created");

        return new CategoryUploadResultDto(file.getOriginalFilename(), recordsBySheet, total);
    }

    private List<Subcategory> registeredSubcategoriesFor(Category category) {
        return Arrays.stream(Subcategory.values())
                .filter(sub -> sub.category() == category)
                .filter(schemaRegistry::isDefined)
                .toList();
    }

    private String sheetName(Subcategory subcategory) {
        return templateWriter.sanitizeSheetName(subcategory.label());
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("The file must have an extension (.xlsx)");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
