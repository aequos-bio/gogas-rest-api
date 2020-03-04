package eu.aequos.gogas.excel.generic;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelDocumentBuilder {

    private Workbook workbook;

    private CellStyle textStyle;
    private CellStyle textCenterStyle;
    private CellStyle intStyle;
    private CellStyle floatStyle;
    private CellStyle dateStyle;
    private CellStyle currencyStyle;
    private CellStyle headerStyle;

    Font boldFont;

    public ExcelDocumentBuilder() {
        initWorkbook();
    }

    private void initWorkbook() {
        workbook = new XSSFWorkbook();

        CreationHelper createHelper = workbook.getCreationHelper();

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

        floatStyle = workbook.createCellStyle();
        floatStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00"));

        currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("0.00 €"));

        intStyle = workbook.createCellStyle();
        intStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0"));

        textStyle = workbook.createCellStyle();

        textCenterStyle = workbook.createCellStyle();
        textCenterStyle.setAlignment(HorizontalAlignment.CENTER);

        boldFont = workbook.createFont();
        boldFont.setBold(true);

        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldFont);
    }

    public <T> ExcelDocumentBuilder addSheet(String title, List<ColumnDefinition<T>> columnDefinitions, List<T> data) {
        new ExcelSheet<T>(this)
                .withTitle(title)
                .withColumnDefinitions(columnDefinitions)
                .withData(data)
                .generate();

        return this;
    }

    public byte[] generate() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        baos.flush();
        return baos.toByteArray();
    }

    public CellStyle getHeaderStyle() {
        return headerStyle;
    }

    public CellStyle getCellStyle(ColumnDefinition columnDefinition) {
        switch (columnDefinition.getDataType()) {
            case Text: return textStyle;
            case TextCenter: return textCenterStyle;
            case Date: return dateStyle;
            case Integer: return intStyle;
            case Float: return floatStyle;
            case Currency: return currencyStyle;
            case SubTotal: return currencyStyle;
            default: throw new UnsupportedOperationException();
        }
    }

    public CellStyle getFooterFont(ColumnDefinition columnDefinition) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.cloneStyleFrom(getCellStyle(columnDefinition));
        cellStyle.setFont(boldFont);
        return cellStyle;
    }

    public Sheet createSheet(String title) {
        return workbook.createSheet(title);
    }
}
