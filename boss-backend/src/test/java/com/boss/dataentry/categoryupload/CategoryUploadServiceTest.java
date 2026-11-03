package com.boss.dataentry.categoryupload;

import com.boss.audit.service.AuditService;
import com.boss.common.access.Category;
import com.boss.common.exception.BusinessValidationException;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.parser.TabularFileReader;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import com.boss.dataentry.schema.SubcategorySchemaRegistry;
import com.boss.dataentry.template.ExcelTemplateWriter;
import com.boss.dataentry.validation.SchemaValidator;
import com.boss.financial.service.FinanceSnapshotSyncService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CategoryUploadServiceTest {

    @Mock
    private SubcategoryDataRecordRepository repository;
    @Mock
    private FinanceSnapshotSyncService financeSnapshotSyncService;
    @Mock
    private AuditService auditService;

    private CategoryUploadService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryUploadService(new SubcategorySchemaRegistry(), new SchemaValidator(),
                new TabularFileReader(), repository, financeSnapshotSyncService, auditService, new ExcelTemplateWriter());
        lenient().when(repository.save(any())).thenAnswer(inv -> {
            SubcategoryDataRecord record = inv.getArgument(0);
            if (record.getId() == null) {
                record.setId(UUID.randomUUID());
            }
            return record;
        });
    }

    @Test
    void humanResourceTemplateHasOneSheetPerRegisteredSubcategoryPlusFieldGuide() throws IOException {
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            assertThat(sheetNames(workbook)).containsExactlyInAnyOrder(
                    "Workforce", "People Cost", "Productivity", "Hiring", "Attrition", "Capability", "Field Guide");
        }
    }

    @Test
    void riskTemplateExcludesMitigation() throws IOException {
        try (Workbook workbook = service.buildTemplate(Category.RISK)) {
            Set<String> names = sheetNames(workbook);
            assertThat(names).contains("Exposure", "Financial", "Operational", "Compliance", "Technology");
            assertThat(names).doesNotContain("Mitigation");
        }
    }

    @Test
    void financeTemplateHasOneSheetPerRegisteredFinanceSubcategoryAndNoLegacyTransactionSheets() throws IOException {
        try (Workbook workbook = service.buildTemplate(Category.FINANCE)) {
            assertThat(sheetNames(workbook)).containsExactlyInAnyOrder(
                    "Position", "Liquidity", "Profitability", "Budget & Forecast", "Cost", "Capital", "Field Guide");
        }
    }

    @Test
    void validRowsAcrossMultipleSheetsAreAllPersisted() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            fillRow(workbook, "Workforce", 1, "2026-03-01", "Sales", "Chennai", "12", "10", "2", "0");
            fillRow(workbook, "Attrition", 1, "Jane Doe", "Sales", "2026-02-15", "VOLUNTARY", "Better opportunity", "18");
            // Every other sheet's own example row is left completely untouched, matching a
            // real user who only fills in the sheets they have data for - it must not
            // create records of its own.
            file = toBytes(workbook);
        }

        var result = service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, multipartFile(file));

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.recordsBySheet()).containsEntry("Workforce", 1).containsEntry("Attrition", 1);
        verify(repository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void oneMissingRequiredFieldAbortsTheWholeUploadAndPersistsNothing() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            // Workforce row is perfectly valid on its own.
            fillRow(workbook, "Workforce", 1, "2026-03-01", "Sales", "Chennai", "12", "10", "2", "0");
            // Attrition row is missing the required "separationDate" (3rd column).
            fillRow(workbook, "Attrition", 1, "Jane Doe", "Sales", "", "VOLUNTARY", "Better opportunity", "18");
            file = toBytes(workbook);
        }

        assertThatThrownBy(() -> service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, multipartFile(file)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Attrition")
                .hasMessageContaining("row 2")
                .hasMessageContaining("Separation date");

        verifyNoInteractions(repository);
    }

    @Test
    void blankRowsAreSkippedRatherThanTreatedAsErrors() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            fillRow(workbook, "Workforce", 1, "2026-03-01", "Sales", "Chennai", "12", "10", "2", "0");
            // Row 3 left completely blank (createRow but no cell values).
            workbook.getSheet("Workforce").createRow(2);
            file = toBytes(workbook);
        }

        var result = service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, multipartFile(file));

        assertThat(result.totalRecords()).isEqualTo(1);
    }

    @Test
    void financeUploadOfAPositionRowTriggersTheSnapshotSync() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.FINANCE)) {
            fillRow(workbook, "Position", 1, "2026-03-01", "500000", "100000", "50000", "900000", "80000", "20000", "1450000");
            // Every other Finance sheet's own example row is left untouched - it must not
            // create a bogus Capital/Profitability record of its own.
            file = toBytes(workbook);
        }

        var result = service.bulkUpload(tenantId, userId, Category.FINANCE, multipartFile(file));

        assertThat(result.recordsBySheet()).containsEntry("Position", 1);
        assertThat(result.recordsBySheet()).doesNotContainKey("Capital");
        verify(financeSnapshotSyncService).sync(eq(userId), any(SubcategoryDataRecord.class));
    }

    @Test
    void rejectsNonExcelFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "date,type\n".getBytes());

        assertThatThrownBy(() -> service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, file))
                .hasMessageContaining(".xlsx");
    }

    @Test
    void uploadingTheTemplateCompletelyUneditedCreatesNoRecords() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            // Upload the template completely as-is - every sheet's example row intact.
            file = toBytes(workbook);
        }

        var result = service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, multipartFile(file));

        assertThat(result.totalRecords()).isEqualTo(0);
        verifyNoInteractions(repository);
    }

    @Test
    void editingEvenOneCellOfTheExampleRowMakesItRealData() throws IOException {
        byte[] file;
        try (Workbook workbook = service.buildTemplate(Category.HUMAN_RESOURCE)) {
            // Only change Workforce's example department - every other cell (and every
            // other sheet) is left exactly as generated.
            workbook.getSheet("Workforce").getRow(1).getCell(1).setCellValue("Engineering");
            file = toBytes(workbook);
        }

        var result = service.bulkUpload(tenantId, userId, Category.HUMAN_RESOURCE, multipartFile(file));

        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.recordsBySheet()).containsEntry("Workforce", 1);
    }

    private Set<String> sheetNames(Workbook workbook) {
        return StreamSupport.stream(workbook.spliterator(), false).map(Sheet::getSheetName).collect(Collectors.toSet());
    }

    private void fillRow(Workbook workbook, String sheetName, int rowNum, String... values) {
        Sheet sheet = workbook.getSheet(sheetName);
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private MockMultipartFile multipartFile(byte[] bytes) throws IOException {
        return new MockMultipartFile("file", "upload.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new ByteArrayInputStream(bytes));
    }
}
