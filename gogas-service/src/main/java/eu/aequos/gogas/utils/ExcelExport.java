package eu.aequos.gogas.utils;

import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserAccountingRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.utils.UserTotal;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import eu.aequos.gogas.service.UserAccountingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ExcelExport {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserAccountingService userAccountingSrv;

    @Autowired
    private UserAccountingRepo userAccountingRepo;

    public byte[] exportUserTotals(List<UserTotal> userTotals, boolean includeUsers) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Situazione contabile utenti");

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        CellStyle headerCellStyle = wb.createCellStyle();
        headerCellStyle.setFont(headerFont);
        Row headerRow = sheet.createRow(0);
        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Disab.");
        h1.setCellStyle(headerCellStyle);
        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("Utente");
        h2.setCellStyle(headerCellStyle);
        Cell h3 = headerRow.createCell(2);
        h3.setCellValue("Saldo");
        h3.setCellStyle(headerCellStyle);

        CellStyle col0style = wb.createCellStyle();
        col0style.setAlignment(HorizontalAlignment.CENTER);
        CellStyle col2style = wb.createCellStyle();
        col2style.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00"));

        int rowNum = 1;

        for (UserTotal tot : userTotals) {
            Row row = sheet.createRow(rowNum++);
            Cell cell0 = row.createCell(0);
            cell0.setCellStyle(col0style);
            cell0.setCellValue(tot.getUser().isEnabled() ? "" : "x");

            row.createCell(1).setCellValue(tot.getUser().getFirstName() + " " + tot.getUser().getLastName());

            Cell cell2 = row.createCell(2);
            cell2.setCellStyle(col2style);
            cell2.setCellValue(tot.getTotal().doubleValue());
        }

        Row row = sheet.createRow(rowNum);
        Cell cell0 = row.createCell(0);
        cell0.setCellValue("");

        Cell cell1 = row.createCell(1);
        cell1.setCellStyle(headerCellStyle);
        cell1.setCellValue("TOTALE");

        Cell cell2 = row.createCell(2);
        CellStyle col2styleBold = wb.createCellStyle();
        col2styleBold.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00"));
        col2styleBold.setFont(headerFont);
        cell2.setCellStyle(col2styleBold);
        cell2.setCellFormula("SUM(C2:C" + rowNum + ")");

        for(int f=0; f<=2; f++)
            sheet.autoSizeColumn(f);

        if (includeUsers) {
            List<User> users = userRepo.findAll();
            Collections.sort(users, (o1, o2) -> {
                int c = o1.getFirstName().compareTo(o2.getFirstName());
                if (c==0)
                    c = o1.getLastName().compareTo(o2.getLastName());
                return c;
            });
            for(User user : users) {
                Sheet usersheet = wb.createSheet(user.getFirstName() + " " + user.getLastName());
                _exportUserDetails(wb, usersheet, user.getId());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        baos.flush();
        return baos.toByteArray();
    }

    public byte[] exportUserDetails(String userId) throws IOException {
        Optional<User> ouser = userRepo.findById(userId);
        if (!ouser.isPresent()) {
            return null;
        }
        User user = ouser.get();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(user.getFirstName() + " " + user.getLastName());

        _exportUserDetails(wb, sheet, userId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        baos.flush();
        return baos.toByteArray();


    }

    private void _exportUserDetails(Workbook wb, Sheet sheet, String userId) throws IOException {

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        CellStyle headerCellStyle = wb.createCellStyle();
        headerCellStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Data");
        h1.setCellStyle(headerCellStyle);
        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("Descrizione");
        h2.setCellStyle(headerCellStyle);
        Cell h3 = headerRow.createCell(2);
        h3.setCellValue("Accrediti");
        h3.setCellStyle(headerCellStyle);
        Cell h4 = headerRow.createCell(3);
        h4.setCellValue("Addebiti");
        h4.setCellStyle(headerCellStyle);
        Cell h5 = headerRow.createCell(4);
        h5.setCellValue("Saldo");
        h5.setCellStyle(headerCellStyle);

        List<UserTransactionFull> ordini = userAccountingRepo.getUserRecordedOrders(userId, userId);
        List<UserTransactionFull> movimenti = userAccountingSrv.getUserTransactions(userId);
        ordini.addAll(movimenti);
        Collections.sort(ordini, new Comparator<UserTransactionFull>() {
            @Override
            public int compare(UserTransactionFull o1, UserTransactionFull o2) {
                int c = 0;
                c = o1.getDate().compareTo(o2.getDate()) * -1;
                if (c==0)
                    c = o1.getDescription().compareTo(o2.getDescription()) * -1;
                return c;
            }
        });

        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
        CellStyle amountStyle = wb.createCellStyle();
        amountStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00"));

        int rowNum = 1;

        for(UserTransactionFull t : ordini) {
            Row row = sheet.createRow(rowNum++);
            Cell cell0 = row.createCell(0);
            cell0.setCellStyle(dateStyle);
            cell0.setCellValue(java.sql.Date.valueOf(t.getDate().toString()));

            row.createCell(1).setCellValue(t.getDescription());

            double amount = t.getAmount().doubleValue() * (t.getSign().equals("-") ? -1 : 1);

            Cell cell2 = row.createCell(2);
            cell2.setCellStyle(amountStyle);
            if (amount>0 || (amount==0 && t.getSign().equals("+")))
                cell2.setCellValue(amount);

            Cell cell3 = row.createCell(3);
            cell3.setCellStyle(amountStyle);
            if (amount<0 || (amount==0 && t.getSign().equals("-")))
                cell3.setCellValue(amount * -1);

            Cell cell4 = row.createCell(4);
            cell4.setCellStyle(amountStyle);
            cell4.setCellFormula("E" + (rowNum+1) + "+C" + rowNum + "-D" + rowNum);
        }

        CellStyle amountStyleBold = wb.createCellStyle();
        amountStyleBold.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00"));
        amountStyleBold.setFont(headerFont);

        Row row = sheet.createRow(rowNum);
        Cell cell0 = row.createCell(0);
        cell0.setCellValue("");

        Cell cell1 = row.createCell(1);
        cell1.setCellStyle(headerCellStyle);
        cell1.setCellValue("TOTALE");

        Cell cell2 = row.createCell(2);
        cell2.setCellStyle(amountStyleBold);
        if (rowNum>1)
            cell2.setCellFormula("SUM(C2:C" + rowNum + ")");

        Cell cell3 = row.createCell(3);
        cell3.setCellStyle(amountStyleBold);
        if (rowNum>1)
            cell3.setCellFormula("SUM(D2:D" + rowNum + ")");

        for(int f=0; f<=4; f++)
            sheet.autoSizeColumn(f);

    }
}
