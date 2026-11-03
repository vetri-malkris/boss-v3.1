package com.boss.dataentry.controller;

import com.boss.common.access.Subcategory;
import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.AuthenticatedUser;
import com.boss.common.security.SecurityUtils;
import com.boss.dataentry.dto.BulkUploadResultDto;
import com.boss.dataentry.dto.CreateDataRecordRequest;
import com.boss.dataentry.dto.DataRecordDto;
import com.boss.dataentry.dto.VoidRecordRequest;
import com.boss.dataentry.service.SubcategoryDataEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One create/list/bulk-upload endpoint set shared by every subcategory that has a
 * data-entry schema (see {@code SubcategorySchemaRegistry}) - {@code subcategory} in
 * the path selects which schema a request is validated against.
 */
@RestController
@RequestMapping("/api/v1/data-entry/{subcategory}")
@Tag(name = "Data entry")
public class SubcategoryDataController {

    private final SubcategoryDataEntryService dataEntryService;

    public SubcategoryDataController(SubcategoryDataEntryService dataEntryService) {
        this.dataEntryService = dataEntryService;
    }

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping("/template")
    @Operation(summary = "Download a single-sheet Excel template for just this subcategory")
    public ResponseEntity<byte[]> template(@PathVariable Subcategory subcategory) {
        try (Workbook workbook = dataEntryService.buildTemplate(subcategory)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            String filename = subcategory.name().toLowerCase(Locale.ROOT) + "-template.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(XLSX)
                    .body(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the template workbook", e);
        }
    }

    @GetMapping("/records")
    @Operation(summary = "List entered records for a subcategory")
    public ApiResponse<List<DataRecordDto>> list(@PathVariable Subcategory subcategory) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return ApiResponse.of(dataEntryService.list(user.getTenantId(), subcategory), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/records")
    @Operation(summary = "Enter one record for a subcategory via its data-entry form")
    public ApiResponse<DataRecordDto> create(@PathVariable Subcategory subcategory,
                                              @Valid @RequestBody CreateDataRecordRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        DataRecordDto record = dataEntryService.create(user.getTenantId(), user.getUserId(), subcategory, request.values());
        return ApiResponse.of(record, MDC.get(LogFields.CORRELATION_ID));
    }

    @PatchMapping("/records/{id}")
    @Operation(summary = "Correct an existing record - the prior values are preserved in the audit trail")
    public ApiResponse<DataRecordDto> update(@PathVariable Subcategory subcategory, @PathVariable UUID id,
                                              @Valid @RequestBody CreateDataRecordRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        DataRecordDto record = dataEntryService.update(user.getTenantId(), user.getUserId(), subcategory, id,
                request.values());
        return ApiResponse.of(record, MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/records/{id}/void")
    @Operation(summary = "Void a mistaken record - never hard-deleted, just removed from the everyday list")
    public ApiResponse<Void> voidRecord(@PathVariable Subcategory subcategory, @PathVariable UUID id,
                                         @Valid @RequestBody VoidRecordRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        dataEntryService.voidRecord(user.getTenantId(), user.getUserId(), subcategory, id, request.reason());
        return ApiResponse.of(null, MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/bulk-upload")
    @Operation(summary = "Bulk-upload a CSV/Excel of records for a subcategory")
    public ApiResponse<BulkUploadResultDto> bulkUpload(@PathVariable Subcategory subcategory,
                                                         @RequestParam("file") MultipartFile file) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        BulkUploadResultDto result = dataEntryService.bulkUpload(user.getTenantId(), user.getUserId(), subcategory, file);
        return ApiResponse.of(result, MDC.get(LogFields.CORRELATION_ID));
    }
}
