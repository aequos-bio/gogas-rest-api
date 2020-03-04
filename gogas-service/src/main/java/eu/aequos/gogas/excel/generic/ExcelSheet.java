package eu.aequos.gogas.excel.generic;

import org.apache.poi.ss.usermodel.*;

import java.util.Date;
import java.util.List;

import static eu.aequos.gogas.excel.generic.ColumnDefinition.DataType.SubTotal;

public class ExcelSheet<T> {

    private int rowCounter = 0;

    private String title;
    private List<ColumnDefinition<T>> columnDefinitions;
    private List<T> data;
    private Sheet sheet;

    private final ExcelDocumentBuilder excelDocument;

    public ExcelSheet(ExcelDocumentBuilder excelDocument) {
        this.excelDocument = excelDocument;
    }

    public ExcelSheet<T> withTitle(String title) {
        this.title = title;
        return this;
    }

    public ExcelSheet<T> withColumnDefinitions(List<ColumnDefinition<T>> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
        return this;
    }

    public ExcelSheet<T> withData(List<T> data) {
        this.data = data;
        return this;
    }

    public void generate() {
        sheet = excelDocument.createSheet(title);

        generateHeader();
        generateBody();
        generateFooter();

        for (int i = 0; i < columnDefinitions.size(); i++)
            resizeColumn(i);
    }

    private void generateHeader() {
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columnDefinitions.size(); i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(columnDefinitions.get(i).getHeader());
            headerCell.setCellStyle(excelDocument.getHeaderStyle());
        }

        rowCounter++;
    }

    private void generateBody() {
        for (T dataItem : data) {
            Row row = sheet.createRow(rowCounter);

            for (int i = 0; i < columnDefinitions.size(); i++) {
                ColumnDefinition<T> columnDefinition = columnDefinitions.get(i);

                Cell cell = row.createCell(i);

                if (columnDefinition.getDataType() == SubTotal)
                    cell.setCellFormula(generateSubTotalFormula(cell));
                else
                    setCellValue(cell, columnDefinition, dataItem);

                cell.setCellStyle(excelDocument.getCellStyle(columnDefinition));
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

    private void generateFooter() {
        Row row = sheet.createRow(rowCounter);

        for (int i = 0; i < columnDefinitions.size(); i++) {
            if (!columnDefinitions.get(i).showTotal())
                continue;

            char columnLetter = getColumnLetter(i);

            Cell cell = row.createCell(i);
            cell.setCellFormula("SUM(" + columnLetter + "2:" + columnLetter + rowCounter + ")");
            cell.setCellStyle(excelDocument.getFooterFont(columnDefinitions.get(i)));
        }
    }

    private char getColumnLetter(int index) {
        return (char) ('A' + index);
    }

    private void resizeColumn(int columnIndex) {
        if (columnDefinitions.get(columnIndex).getDataType() == SubTotal) {
            sheet.setColumnWidth(columnIndex, 10 * 256);
            return;
        }

        sheet.autoSizeColumn(columnIndex);
    }
}
