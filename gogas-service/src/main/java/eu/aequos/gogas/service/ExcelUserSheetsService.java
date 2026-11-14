package eu.aequos.gogas.service;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Excel (.xlsx) variant of the per-user order "sheets", mirroring the layout of
 * the legacy PrtSchedeUtentiOrdine.xls: one worksheet per ordering user, each
 * containing the user position, an order/user header, a product table grouped
 * by category, and the three footer labels (totals / boxes / packed-by) left
 * blank for hand-filling.
 *
 * The data comes from the same {@link UserOrderSheetsExtractor} used by the PDF
 * export, so the two outputs stay consistent.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelUserSheetsService {

    /** Column widths (in POI units: 1/256th of a character) tuned to the sample. */
    private static final int[] COLUMN_WIDTHS = {
            473,   // A - thin margin
            10312, // B - product name
            5488,  // C
            361,   // D
            1535,  // E - UM
            2228,  // F - price
            2484,  // G - ordered qty
            2961,  // H - delivered qty
    };

    private static final DecimalFormat PRICE_FORMAT =
            new DecimalFormat("€ 0.00", new DecimalFormatSymbols(Locale.ITALIAN));

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserOrderSheetsExtractor userOrderSheetsExtractor;

    public byte[] generateUserSheets(Order order) {
        List<UserOrderSheetData> userSheets = userOrderSheetsExtractor.extract(order);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = new Styles(workbook);

            int sheetIndex = 1;
            for (UserOrderSheetData userSheet : userSheets) {
                Sheet sheet = workbook.createSheet("Sheet" + sheetIndex++);
                buildUserSheet(sheet, styles, userSheet, order);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Error while generating user Excel sheets for order " + order.getId(), ex);
            throw new GoGasException();
        }
    }

    private void buildUserSheet(Sheet sheet, Styles styles, UserOrderSheetData userSheet, Order order) {
        for (int c = 0; c < COLUMN_WIDTHS.length; c++) {
            sheet.setColumnWidth(c, COLUMN_WIDTHS[c]);
        }

        int rowIdx = 0;

        // Row 1: top margin
        sheet.createRow(rowIdx++);

        // Row 2: user position (cell C)
        Row positionRow = sheet.createRow(rowIdx++);
        Cell positionCell = positionRow.createCell(2);
        positionCell.setCellValue(userSheet.getPosition());
        positionCell.setCellStyle(styles.position);

        // Row 3: separator
        sheet.createRow(rowIdx++);

        // Row 4: order/user header, merged B:H
        Row headerRow = sheet.createRow(rowIdx);
        String headerText = "Ordine " + order.getOrderType().getDescription() +
                " in consegna " + DATE_FORMAT.format(order.getDeliveryDate()) +
                " - " + userSheet.getFirstName() + " " + userSheet.getLastName();
        createMergedCell(sheet, headerRow, rowIdx, 1, 7, headerText, styles.title);
        rowIdx++;

        // Row 5: column headers
        Row columnHeaderRow = sheet.createRow(rowIdx);
        createMergedCell(sheet, columnHeaderRow, rowIdx, 1, 3, "Prodotto", styles.headerLeft);
        setCell(columnHeaderRow, 4, "UM", styles.headerCenter);
        setCell(columnHeaderRow, 5, "Prezzo al Kg", styles.headerCenter);
        setCell(columnHeaderRow, 6, "Qta\nordinata", styles.headerCenter);
        setCell(columnHeaderRow, 7, "Qta\nconsegnata", styles.headerCenter);
        rowIdx++;

        // Product rows grouped by category. Alternating rows are shaded,
        // matching the PDF (odd item index -> highlighted background).
        String currentCategory = "";
        List<UserOrderSheetData.Item> items = userSheet.getItems();
        for (int index = 0; index < items.size(); index++) {
            UserOrderSheetData.Item item = items.get(index);

            if (!currentCategory.equals(item.getCategory())) {
                Row categoryRow = sheet.createRow(rowIdx);
                createMergedCell(sheet, categoryRow, rowIdx, 1, 7, item.getCategory(), styles.category);
                rowIdx++;
                currentCategory = item.getCategory();
            }

            boolean odd = index % 2 == 1;
            CellStyle textStyle = odd ? styles.textOdd : styles.text;
            CellStyle centerStyle = odd ? styles.centerOdd : styles.center;

            Row row = sheet.createRow(rowIdx);
            createMergedCell(sheet, row, rowIdx, 1, 3, item.getProductName(), textStyle);
            setCell(row, 4, item.getUm(), centerStyle);
            setCell(row, 5, PRICE_FORMAT.format(item.getUnitPrice()), centerStyle);
            setQuantityCell(row, 6, item.getFormattedQuantity(), centerStyle);
            setCell(row, 7, "", centerStyle); // Qta consegnata left blank
            rowIdx++;
        }

        // Spacer row
        sheet.createRow(rowIdx++);

        // Footer rows: label (B:F) + empty (G:H)
        rowIdx = addFooterRow(sheet, styles, rowIdx, "Totale da pagare:");
        rowIdx = addFooterRow(sheet, styles, rowIdx, "Numero di cassette da ritirare:");
        addFooterRow(sheet, styles, rowIdx, "Cassette confezionate per te da:");
    }

    private int addFooterRow(Sheet sheet, Styles styles, int rowIdx, String label) {
        Row row = sheet.createRow(rowIdx);
        createMergedCell(sheet, row, rowIdx, 1, 5, label, styles.footer);
        createMergedCell(sheet, row, rowIdx, 6, 7, "", styles.footerEmpty);
        return rowIdx + 1;
    }

    /**
     * Render the ordered quantity. Full-box quantities come through as text
     * ("1 cassa"); plain quantities are numeric so Excel treats them as numbers,
     * matching the sample.
     */
    private void setQuantityCell(Row row, int col, String formattedQuantity, CellStyle style) {
        Cell cell = row.createCell(col);
        Double numeric = parseNumber(formattedQuantity);
        if (numeric != null) {
            cell.setCellValue(numeric);
        } else {
            cell.setCellValue(formattedQuantity);
        }
        cell.setCellStyle(style);
    }

    private static Double parseNumber(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(',', '.')).doubleValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void createMergedCell(Sheet sheet, Row row, int rowIdx, int firstCol, int lastCol,
                                  String value, CellStyle style) {
        Cell cell = row.createCell(firstCol);
        cell.setCellValue(value);
        cell.setCellStyle(style);

        // Style the covered cells too so borders render across the merge.
        for (int c = firstCol + 1; c <= lastCol; c++) {
            Cell covered = row.createCell(c);
            covered.setCellStyle(style);
        }

        if (lastCol > firstCol) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, firstCol, lastCol));
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** Pre-built cell styles for a workbook. */
    private static class Styles {
        final CellStyle position;
        final CellStyle title;
        final CellStyle headerLeft;
        final CellStyle headerCenter;
        final CellStyle category;
        final CellStyle text;
        final CellStyle center;
        final CellStyle textOdd;
        final CellStyle centerOdd;
        final CellStyle footer;
        final CellStyle footerEmpty;

        /** Background for alternating (odd) product rows, matching the PDF. */
        private static final byte[] ODD_ROW_RGB = new byte[] { (byte) 244, (byte) 245, (byte) 220 };

        Styles(Workbook workbook) {
            Font normal = workbook.createFont();
            normal.setFontHeightInPoints((short) 9);

            Font bold = workbook.createFont();
            bold.setBold(true);
            bold.setFontHeightInPoints((short) 9);

            Font titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 12);

            Font positionFont = workbook.createFont();
            positionFont.setFontHeightInPoints((short) 14);

            Font footerFont = workbook.createFont();
            footerFont.setBold(true);
            footerFont.setItalic(true);
            footerFont.setFontHeightInPoints((short) 9);

            Font categoryFont = workbook.createFont();
            categoryFont.setItalic(true);
            categoryFont.setFontHeightInPoints((short) 9);

            position = base(workbook, positionFont, HorizontalAlignment.CENTER, false);

            title = base(workbook, titleFont, HorizontalAlignment.CENTER, true);

            headerLeft = base(workbook, bold, HorizontalAlignment.LEFT, true);
            headerCenter = base(workbook, bold, HorizontalAlignment.CENTER, true);
            headerCenter.setWrapText(true);
            headerLeft.setWrapText(true);

            category = base(workbook, categoryFont, HorizontalAlignment.LEFT, true);
            category.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            category.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            text = base(workbook, normal, HorizontalAlignment.LEFT, true);
            center = base(workbook, normal, HorizontalAlignment.CENTER, true);

            XSSFColor oddColor = new XSSFColor(ODD_ROW_RGB, null);
            textOdd = withOddFill(base(workbook, normal, HorizontalAlignment.LEFT, true), oddColor);
            centerOdd = withOddFill(base(workbook, normal, HorizontalAlignment.CENTER, true), oddColor);

            footer = base(workbook, footerFont, HorizontalAlignment.RIGHT, false);
            footerEmpty = base(workbook, normal, HorizontalAlignment.CENTER, true);
        }

        private static CellStyle withOddFill(CellStyle style, XSSFColor color) {
            ((XSSFCellStyle) style).setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private static CellStyle base(Workbook workbook, Font font, HorizontalAlignment alignment, boolean bordered) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if (bordered) {
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
            }
            return style;
        }
    }
}
