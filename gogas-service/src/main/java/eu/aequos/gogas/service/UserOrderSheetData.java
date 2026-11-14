package eu.aequos.gogas.service;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data needed to render a single user's order "sheet" (one PDF page / one Excel
 * worksheet). Shared by {@link PdfReportService} and {@link ExcelUserSheetsService}
 * so both outputs are built from the exact same data and business rules.
 */
@Data
public class UserOrderSheetData {

    /** The user the sheet belongs to. */
    private final String firstName;
    private final String lastName;
    private final int position;

    /** The ordered rows (already grouped/ordered by category then price list). */
    private final List<Item> items;

    @Data
    public static class Item {
        private final String category;
        private final String productName;
        private final String um;
        private final BigDecimal unitPrice;
        /** Pre-formatted ordered quantity ("N casse" for full boxes, else "0,000"). */
        private final String formattedQuantity;
    }
}
