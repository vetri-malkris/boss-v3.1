package com.boss.dataentry.template;

import com.boss.dataentry.domain.FieldDefinition;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the header row (and its "Field Guide" legend) of a downloadable upload
 * template - shared by the per-category workbook ({@code CategoryUploadService})
 * and the per-subcategory single-sheet template ({@code SubcategoryDataEntryService})
 * so both produce sheets in exactly the same shape and stay in sync automatically.
 */
@Component
public class ExcelTemplateWriter {

    /** One sheet's name plus the fields it should have a column for. */
    public record SheetSpec(String name, List<FieldDefinition> fields) {
    }

    /**
     * Adds one sheet with a header row matching {@code field.name()} for each field
     * (required fields in bold red, optional fields in plain bold) - the header text
     * is always the machine field name, never the human label, so it matches exactly
     * what {@code SchemaValidator} expects back on upload. Row 2 is a
     * syntactically-valid example row (styled in muted italics)
     * illustrating the expected format for each column - {@link #isUnmodifiedExampleRow}
     * lets an upload recognize and skip it as long as it's still exactly what was
     * generated here, without relying on any hidden state that a direct edit could
     * leave stale (a hidden marker cell would survive the user typing over the
     * visible values, silently discarding their real data - this doesn't).
     */
    public void writeHeaderSheet(Workbook workbook, String sheetName, List<FieldDefinition> fields) {
        CellStyle requiredStyle = requiredHeaderStyle(workbook);
        CellStyle optionalStyle = optionalHeaderStyle(workbook);
        CellStyle exampleStyle = exampleRowStyle(workbook);
        Sheet sheet = workbook.createSheet(sanitizeSheetName(sheetName));
        Row header = sheet.createRow(0);
        Row example = sheet.createRow(1);
        for (int i = 0; i < fields.size(); i++) {
            FieldDefinition field = fields.get(i);
            Cell headerCell = header.createCell(i);
            headerCell.setCellValue(field.name());
            headerCell.setCellStyle(field.required() ? requiredStyle : optionalStyle);
            sheet.setColumnWidth(i, Math.max(3500, (field.name().length() + 4) * 300));

            Cell exampleCell = example.createCell(i);
            exampleCell.setCellValue(exampleValue(field));
            exampleCell.setCellStyle(exampleStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    /**
     * True only if every column in this parsed row still holds exactly the example
     * value {@link #writeHeaderSheet} generated for it - i.e. the user left row 2
     * completely untouched. The moment any single cell is edited (including being
     * cleared), the row no longer matches and is treated as real data, so overwriting
     * the example row in place is just as safe as adding new rows below it.
     */
    public boolean isUnmodifiedExampleRow(List<FieldDefinition> fields, Map<String, String> row) {
        for (FieldDefinition field : fields) {
            String actual = row.get(field.name().toLowerCase(Locale.ROOT));
            if (actual == null || !actual.trim().equals(exampleValue(field))) {
                return false;
            }
        }
        return true;
    }

    /** A trailing "Field Guide" sheet explaining every column across every sheet
     * passed in - what it means, whether it's required, its type, and (for ENUM
     * fields) its allowed values. */
    public void writeFieldGuide(Workbook workbook, List<SheetSpec> sheets) {
        Sheet sheet = workbook.createSheet("Field Guide");
        sheet.createRow(0).createCell(0).setCellValue(
                "Row 2 of every data sheet is an example showing the expected format for that sheet. "
                        + "Left exactly as downloaded, it is ignored on upload. Overwrite it with your own data "
                        + "(or add new rows below it) to include that row for real - editing even one cell is "
                        + "enough for it to count as your data.");

        Row header = sheet.createRow(1);
        String[] columns = {"Sheet", "Column name", "Meaning", "Required", "Type", "Allowed values"};
        CellStyle boldStyle = requiredHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(boldStyle);
            sheet.setColumnWidth(i, 6000);
        }

        int rowNum = 2;
        for (SheetSpec spec : sheets) {
            for (FieldDefinition field : spec.fields()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(spec.name());
                row.createCell(1).setCellValue(field.name());
                row.createCell(2).setCellValue(field.label());
                row.createCell(3).setCellValue(field.required() ? "Required" : "Optional");
                row.createCell(4).setCellValue(field.type().name());
                row.createCell(5).setCellValue(field.options().isEmpty() ? "" : String.join(", ", field.options()));
            }
        }
    }

    public String sanitizeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", "-");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private CellStyle requiredHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle optionalHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle exampleRowStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    /** A generic, always-valid example per field type - not domain-specific, but
     * enough to show the expected format (and, for ENUM fields, a real allowed
     * value) without hand-authoring an example for every field across every
     * subcategory. */
    private String exampleValue(FieldDefinition field) {
        return switch (field.type()) {
            case TEXT -> "Sample text";
            case NUMBER -> "100";
            case DATE -> "15-01-2026";
            case ENUM -> field.options().isEmpty() ? "" : field.options().get(0);
        };
    }
}
