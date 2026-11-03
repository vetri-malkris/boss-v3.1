package com.boss.dataentry.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.dataentry.dto.SubcategorySchemaDto;
import com.boss.dataentry.schema.SubcategorySchemaRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Which subcategories have a real data-entry form, and what fields each one has -
 * the frontend renders its data-entry form/bulk-upload UI purely from this, never
 * hardcoding a per-subcategory field list of its own.
 */
@RestController
@RequestMapping("/api/v1/data-entry/schemas")
@Tag(name = "Data entry")
public class SubcategorySchemaController {

    private final SubcategorySchemaRegistry schemaRegistry;

    public SubcategorySchemaController(SubcategorySchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @GetMapping
    @Operation(summary = "Every subcategory that has a real data-entry form, with its field schema")
    public ApiResponse<List<SubcategorySchemaDto>> list() {
        List<SubcategorySchemaDto> schemas = schemaRegistry.all().stream().map(SubcategorySchemaDto::from).toList();
        return ApiResponse.of(schemas, MDC.get(LogFields.CORRELATION_ID));
    }
}
