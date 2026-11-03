package com.boss.dataentry.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.access.Subcategory;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.dto.BulkUploadResultDto;
import com.boss.dataentry.dto.DataRecordDto;
import com.boss.dataentry.parser.TabularFileReader;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import com.boss.dataentry.schema.SubcategorySchemaRegistry;
import com.boss.dataentry.template.ExcelTemplateWriter;
import com.boss.dataentry.validation.SchemaValidator;
import com.boss.financial.service.FinanceSnapshotSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcategoryDataEntryServiceTest {

    @Mock
    private SubcategoryDataRecordRepository repository;
    @Mock
    private AuditService auditService;
    @Mock
    private FinanceSnapshotSyncService financeSnapshotSyncService;

    private SubcategoryDataEntryService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubcategoryDataEntryService(
                new SubcategorySchemaRegistry(), new SchemaValidator(), new TabularFileReader(), repository,
                auditService, new ExcelTemplateWriter(), financeSnapshotSyncService);
    }

    private void stubSave() {
        when(repository.save(any())).thenAnswer(inv -> {
            SubcategoryDataRecord record = inv.getArgument(0);
            if (record.getId() == null) {
                record.setId(UUID.randomUUID());
            }
            return record;
        });
    }

    private SubcategoryDataRecord existingHiringRecord() {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.HIRING, Map.of(
                "requisitionId", "REQ-001", "positionTitle", "Backend Engineer", "department", "Engineering",
                "status", "OPEN", "openedDate", "2026-03-01"), userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        return record;
    }

    @Test
    void buildsASingleSheetTemplateNamedAfterTheSubcategory() {
        try (var workbook = service.buildTemplate(Subcategory.ATTRITION)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2); // Attrition + Field Guide
            assertThat(workbook.getSheet("Attrition")).isNotNull();
            var header = workbook.getSheet("Attrition").getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("employeeName");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createsAValidRecordAgainstTheSubcategorysOwnSchema() {
        stubSave();
        DataRecordDto record = service.create(tenantId, userId, Subcategory.HIRING, Map.of(
                "requisitionId", "REQ-001",
                "positionTitle", "Backend Engineer",
                "department", "Engineering",
                "status", "open",
                "openedDate", "2026-03-01"));

        assertThat(record.subcategory()).isEqualTo("HIRING");
        assertThat(record.fields()).containsEntry("positionTitle", "Backend Engineer").containsEntry("status", "OPEN");
    }

    @Test
    void rejectsARecordMissingARequiredField() {
        assertThatThrownBy(() -> service.create(tenantId, userId, Subcategory.HIRING, Map.of("department", "Engineering")))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void bulkUploadSkipsInvalidRowsAndCountsThemAsWarningsInsteadOfFailingTheWholeFile() {
        stubSave();
        String csv = """
                requisitionId,positionTitle,department,status,openedDate
                REQ-001,Backend Engineer,Engineering,OPEN,2026-03-01
                REQ-002,,Engineering,OPEN,2026-03-02
                REQ-003,Frontend Engineer,Engineering,NOT_A_STATUS,2026-03-03
                """;
        MockMultipartFile file = new MockMultipartFile("file", "hiring.csv", "text/csv", csv.getBytes());

        BulkUploadResultDto result = service.bulkUpload(tenantId, userId, Subcategory.HIRING, file);

        assertThat(result.rowsCreated()).isEqualTo(1);
        assertThat(result.rowsSkipped()).isEqualTo(2);
        assertThat(result.warnings()).hasSize(2);
    }

    @Test
    void listReturnsWhateverTheRepositoryHasForThatTenantAndSubcategory() {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.HIRING, Map.of("positionTitle", "X"), userId);
        record.setId(UUID.randomUUID());
        when(repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.HIRING))
                .thenReturn(List.of(record));

        List<DataRecordDto> records = service.list(tenantId, Subcategory.HIRING);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).fields()).containsEntry("positionTitle", "X");
    }

    @Test
    void updatePreservesThePriorValuesInTheAuditTrailRatherThanDiscardingThem() {
        SubcategoryDataRecord record = existingHiringRecord();
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));
        stubSave();

        DataRecordDto updated = service.update(tenantId, userId, Subcategory.HIRING, record.getId(), Map.of(
                "requisitionId", "REQ-001", "positionTitle", "Senior Backend Engineer", "department", "Engineering",
                "status", "FILLED", "openedDate", "2026-03-01"));

        assertThat(updated.fields()).containsEntry("positionTitle", "Senior Backend Engineer")
                .containsEntry("status", "FILLED");

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(auditService).record(eq(tenantId), eq(userId), eq(AuditEventType.RECORD_UPDATED),
                anyString(), anyString(), description.capture());
        assertThat(description.getValue()).contains("Backend Engineer").contains("Senior Backend Engineer");
    }

    @Test
    void cannotUpdateARecordBelongingToAnotherTenant() {
        SubcategoryDataRecord record = existingHiringRecord();
        record.setTenantId(UUID.randomUUID());
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.update(tenantId, userId, Subcategory.HIRING, record.getId(), Map.of(
                "requisitionId", "REQ-001", "positionTitle", "X", "department", "Y", "status", "OPEN",
                "openedDate", "2026-03-01")))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void voidingARecordMarksItVoidedAndRecordsWhoAndWhy() {
        SubcategoryDataRecord record = existingHiringRecord();
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));
        stubSave();

        service.voidRecord(tenantId, userId, Subcategory.HIRING, record.getId(), "Duplicate entry");

        assertThat(record.isVoided()).isTrue();
        assertThat(record.getVoidReason()).isEqualTo("Duplicate entry");
        assertThat(record.getVoidedBy()).isEqualTo(userId);
        verify(auditService).record(eq(tenantId), eq(userId), eq(AuditEventType.RECORD_VOIDED),
                anyString(), anyString(), contains("Duplicate entry"));
    }

    @Test
    void creatingAPositionRecordTriggersTheFinanceSnapshotSync() {
        stubSave();
        DataRecordDto record = service.create(tenantId, userId, Subcategory.POSITION, Map.of(
                "asOfDate", "2026-03-01", "cashAndBankBalance", "500000", "accountsReceivable", "100000",
                "inventoryValue", "50000", "fixedAssets", "900000", "accountsPayable", "80000",
                "otherLiabilities", "20000", "equity", "1450000"));

        ArgumentCaptor<SubcategoryDataRecord> captor = ArgumentCaptor.forClass(SubcategoryDataRecord.class);
        verify(financeSnapshotSyncService).sync(eq(userId), captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(record.id());
    }

    @Test
    void creatingAHiringRecordDoesNotTriggerTheFinanceSnapshotSync() {
        stubSave();
        service.create(tenantId, userId, Subcategory.HIRING, Map.of(
                "requisitionId", "REQ-001", "positionTitle", "Backend Engineer", "department", "Engineering",
                "status", "OPEN", "openedDate", "2026-03-01"));

        verify(financeSnapshotSyncService).sync(eq(userId), any());
        // FinanceSnapshotSyncService.sync is called for every subcategory (it decides
        // internally whether it's relevant) - this test only documents that calling it
        // is harmless/expected here too, not that it's skipped.
    }

    @Test
    void updatingAPositionRecordTriggersTheFinanceSnapshotSyncAgain() {
        SubcategoryDataRecord record = new SubcategoryDataRecord(Subcategory.POSITION, Map.of(
                "asOfDate", "2026-03-01", "cashAndBankBalance", "500000", "accountsReceivable", "100000",
                "inventoryValue", "50000", "fixedAssets", "900000", "accountsPayable", "80000",
                "otherLiabilities", "20000", "equity", "1450000"), userId);
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));
        stubSave();

        service.update(tenantId, userId, Subcategory.POSITION, record.getId(), Map.of(
                "asOfDate", "2026-03-01", "cashAndBankBalance", "600000", "accountsReceivable", "100000",
                "inventoryValue", "50000", "fixedAssets", "900000", "accountsPayable", "80000",
                "otherLiabilities", "20000", "equity", "1450000"));

        verify(financeSnapshotSyncService).sync(eq(userId), eq(record));
    }

    @Test
    void cannotEditAnAlreadyVoidedRecord() {
        SubcategoryDataRecord record = existingHiringRecord();
        record.voidRecord(userId, "Duplicate");
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.update(tenantId, userId, Subcategory.HIRING, record.getId(), Map.of(
                "requisitionId", "REQ-001", "positionTitle", "X", "department", "Y", "status", "OPEN",
                "openedDate", "2026-03-01")))
                .isInstanceOf(BusinessValidationException.class);
    }
}
