package eu.aequos.gogas.excel.generic;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static eu.aequos.gogas.excel.generic.ColumnDefinition.DataType.SubTotal;

public class GenericExcelGenerator<T> {

    private final String title;
    private final List<ColumnDefinition<T>> columnDefinitions;

    private Workbook workbook;
    private Sheet sheet;

    private CellStyle textStyle;
    private CellStyle textCenterStyle;
    private CellStyle intStyle;
    private CellStyle floatStyle;
    private CellStyle dateStyle;
    private CellStyle currencyStyle;

    Font boldFont;

    private int rowCounter = 0;

    public GenericExcelGenerator(String title, List<ColumnDefinition<T>> columnDefinitions) {
        this.title = title;
        this.columnDefinitions = columnDefinitions;
    }

    public byte[] generate(List<T> data) throws IOException {
        initWorkbook();

        generateHeader();
        generateBody(data);
        generateFooter();

        finalizeWorkbook();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        baos.flush();
        return baos.toByteArray();
    }

    private void initWorkbook() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(title);

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
    }

    private void generateHeader() {
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(boldFont);

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columnDefinitions.size(); i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(columnDefinitions.get(i).getHeader());
            headerCell.setCellStyle(headerCellStyle);
        }

        rowCounter++;
    }

    private void generateBody(List<T> data) {
        for (T dataItem : data) {
            Row row = sheet.createRow(rowCounter);

            for (int i = 0; i < columnDefinitions.size(); i++) {
                ColumnDefinition<T> columnDefinition = columnDefinitions.get(i);

                Cell cell = row.createCell(i);

                if (columnDefinition.getDataType() == SubTotal)
                    cell.setCellFormula(generateSubTotalFormula(cell));
                else
                    setCellValue(cell, columnDefinition, dataItem);

                cell.setCellStyle(getCellStyle(columnDefinition));
            }

            rowCounter++;
        }
    }

    private void setCellValue(Cell cell, ColumnDefinition<T> columnDefinition, T dataItem) {
        Object extractedFieldValue = columnDefinition.extract(dataItem);

        if (extractedFieldValue == null)
            return;

        switch (columnDefinition.getDataType()) {
            case Text:
            case TextCenter:
                cell.setCellValue((String) extractedFieldValue);
                break;

            case Date:
                cell.setCellValue((Date) extractedFieldValue);
                break;

            case Integer:
            case Float:
            case Currency:
                cell.setCellValue((Double) extractedFieldValue);
                break;

            default:
                throw new UnsupportedOperationException();
        }
    }

    private String generateSubTotalFormula(Cell cell) {
        String prevSubTotalCell;

        if (rowCounter == 1)
            prevSubTotalCell = "0";
        else
            prevSubTotalCell = getColumnLetter(cell.getColumnIndex()) + "" + rowCounter;

        String negativeCell = getColumnLetter(cell.getColumnIndex()-1) + "" + (rowCounter+1);
        String positiveCell = getColumnLetter(cell.getColumnIndex()-2) + "" + (rowCounter+1);

        return prevSubTotalCell + " + " + positiveCell + " - " + negativeCell;
    }

    private CellStyle getCellStyle(ColumnDefinition<T> columnDefinition) {
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

    private void generateFooter() {
        Row row = sheet.createRow(rowCounter);

        for (int i = 0; i < columnDefinitions.size(); i++) {
            if (!columnDefinitions.get(i).showTotal())
                continue;

            char columnLetter = getColumnLetter(i);

            Cell cell = row.createCell(i);
            cell.setCellFormula("SUM(" + columnLetter + "2:" + columnLetter + rowCounter + ")");

            CellStyle footerStyle = workbook.createCellStyle();
            footerStyle.cloneStyleFrom(getCellStyle(columnDefinitions.get(i)));
            footerStyle.setFont(boldFont);

            cell.setCellStyle(footerStyle);
        }
    }

    private char getColumnLetter(int index) {
        return (char) ('A' + index);
    }

    private void finalizeWorkbook() {
        for (int i = 0; i < columnDefinitions.size(); i++)
            sheet.autoSizeColumn(i);
    }
}
