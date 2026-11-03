package com.boss.dataentry.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generic CSV/Excel reader shared by every subcategory's bulk upload: a header row
 * (case-insensitive, any column order) plus data rows, returned as one
 * case-insensitive-keyed map per row. Domain-specific interpretation of those values
 * (which columns matter, what type each one is) is the caller's job - see
 * {@code SubcategoryDataEntryService}. Reuses the same NaN/formula-safe cell handling
 * as the transaction importer (a formula error like =1/0 must not fail the whole file).
 */
@Component
public class TabularFileReader {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public List<Map<String, String>> read(InputStream input, String extension) throws IOException {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "csv" -> readCsv(input);
            case "xlsx", "xls" -> readExcel(input);
            default -> throw new IllegalArgumentException(
                    "Unsupported file type '." + extension + "'. Upload a .csv or .xlsx file.");
        };
    }

    private List<Map<String, String>> readCsv(InputStream input) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             var parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                record.toMap().forEach((key, value) -> row.put(key.toLowerCase(Locale.ROOT), value));
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, String>> readExcel(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            return readSheet(workbook.getSheetAt(0));
        }
    }

    /**
     * Reads every sheet in a workbook, keyed by its exact sheet name - used by
     * multi-sheet bulk uploads (e.g. a whole category's worth of subcategories in
     * one file) where {@link #read} only ever looks at the first sheet.
     */
    public Map<String, List<Map<String, String>>> readAllSheets(InputStream input) throws IOException {
        Map<String, List<Map<String, String>>> bySheet = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(input)) {
            for (Sheet sheet : workbook) {
                bySheet.put(sheet.getSheetName(), readSheet(sheet));
            }
        }
        return bySheet;
    }

    private List<Map<String, String>> readSheet(Sheet sheet) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
            return rows;
        }

        Map<Integer, String> columnNames = readHeader(sheet.getRow(sheet.getFirstRowNum()));

        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlankRow(row)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            columnNames.forEach((index, name) -> values.put(name, cellToString(row.getCell(index))));
            rows.add(values);
        }
        return rows;
    }

    private Map<Integer, String> readHeader(Row headerRow) {
        Map<Integer, String> columnNames = new LinkedHashMap<>();
        if (headerRow == null) {
            return columnNames;
        }
        for (Cell cell : headerRow) {
            String name = cellToString(cell);
            if (name != null && !name.isBlank()) {
                columnNames.put(cell.getColumnIndex(), name.trim().toLowerCase(Locale.ROOT));
            }
        }
        return columnNames;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            String value = cellToString(cell);
            if (cell.getCellType() != CellType.BLANK && value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> numericCellToString(cell);
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> formulaCellToString(cell);
            default -> null;
        };
    }

    private String numericCellToString(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().format(ISO_DATE);
        }
        double value = cell.getNumericCellValue();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String formulaCellToString(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> numericCellToString(cell);
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
