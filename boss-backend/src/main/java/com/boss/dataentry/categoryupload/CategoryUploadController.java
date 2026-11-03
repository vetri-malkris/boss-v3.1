package com.boss.dataentry.categoryupload;

import com.boss.common.access.Category;
import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.common.security.AuthenticatedUser;
import com.boss.common.security.SecurityUtils;
import com.boss.dataentry.categoryupload.dto.CategoryUploadResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * One downloadable template + one all-or-nothing bulk upload per top-level category
 * (see {@link CategoryUploadService}) - distinct from the per-subcategory bulk
 * upload in {@code SubcategoryDataController}, which stays skip-bad-rows.
 */
@RestController
@RequestMapping("/api/v1/data-entry/category/{category}")
@Tag(name = "Data entry")
public class CategoryUploadController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CategoryUploadService service;

    public CategoryUploadController(CategoryUploadService service) {
        this.service = service;
    }

    @GetMapping("/template")
    @Operation(summary = "Download a multi-sheet Excel template for every uploadable subcategory in a category")
    public ResponseEntity<byte[]> template(@PathVariable Category category) {
        try (Workbook workbook = service.buildTemplate(category)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            String filename = category.name().toLowerCase(Locale.ROOT) + "-template.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(XLSX)
                    .body(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the template workbook", e);
        }
    }

    @PostMapping("/bulk-upload")
    @Operation(summary = "Upload a filled multi-sheet Excel template - all-or-nothing across every sheet")
    public ApiResponse<CategoryUploadResultDto> bulkUpload(@PathVariable Category category,
                                                            @RequestParam("file") MultipartFile file) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        CategoryUploadResultDto result = service.bulkUpload(user.getTenantId(), user.getUserId(), category, file);
        return ApiResponse.of(result, MDC.get(LogFields.CORRELATION_ID));
    }
}
