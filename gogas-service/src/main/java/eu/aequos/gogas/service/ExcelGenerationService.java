package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.excel.ExcelServiceClient;
import eu.aequos.gogas.excel.generic.ColumnDefinition;
import eu.aequos.gogas.excel.generic.GenericExcelGenerator;
import eu.aequos.gogas.excel.order.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.excel.products.ExcelPriceListItem;
import eu.aequos.gogas.persistence.utils.UserTotal;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.aequos.gogas.excel.generic.ColumnDefinition.DataType.*;

@Slf4j
@Service
public class ExcelGenerationService {

    ExcelServiceClient excelServiceClient;

    UserRepo userRepo;
    OrderItemRepo orderItemRepo;
    ProductRepo productRepo;
    SupplierOrderItemRepo supplierOrderItemRepo;
    UserService userService;

    public ExcelGenerationService(ExcelServiceClient excelServiceClient, UserRepo userRepo, UserService userService,
                                  OrderItemRepo orderItemRepo, ProductRepo productRepo,
                                  SupplierOrderItemRepo supplierOrderItemRepo) {
        this.excelServiceClient = excelServiceClient;
        this.userRepo = userRepo;
        this.userService = userService;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
    }

    public byte[] extractProductPriceList(String orderTypeId) throws GoGasException {
        List<ExcelPriceListItem> products = productRepo.findByType(orderTypeId).stream()
                .map(this::convertProductForPriceListExport)
                .collect(Collectors.toList());

        try {
            return excelServiceClient.products(products);
        } catch(Exception ex) {
            log.error("Error while calling excel service", ex);
            throw new GoGasException("Error while generating excel file");
        }
    }

    private ExcelPriceListItem convertProductForPriceListExport(Product p) {
        ExcelPriceListItem exp = new ExcelPriceListItem();
        exp.setExternalId(p.getExternalId());
        exp.setName(p.getDescription());
        exp.setSupplierExternalId(p.getSupplier().getExternalId());
        exp.setSupplierName(p.getSupplier().getName());
        exp.setSupplierProvince(p.getSupplier().getProvince());
        exp.setCategory(p.getCategory().getDescription());
        exp.setUnitOfMeasure(p.getUm());
        exp.setUnitPrice(p.getPrice());
        exp.setBoxWeight(p.getBoxWeight());
        exp.setNotes(p.getNotes());
        exp.setFrequency(p.getFrequency());
        exp.setWholeBoxesOnly(p.isBoxOnly());
        exp.setMultiple(p.getMultiple());
        return exp;
    }

    public byte[] extractOrderDetails(Order order) throws ItemNotFoundException, GoGasException {
        List<OrderItemExport> orderItems = orderItemRepo.findByOrderAndSummary(order.getId(), true).stream()
                .map(this::getOrderItemsForExport)
                .collect(Collectors.toList());

        List<UserExport> userList = getUsersForExport(orderItems, order.getOrderType().isExcelAllUsers()).stream()
                .map(this::convertUserForExport)
                .sorted(Comparator.comparing(UserExport::getFullName))
                .collect(Collectors.toList());

        List<ProductExport> products = getProductsForExport(orderItems, order.getOrderType()).stream()
                .map(this::convertProductForExport)
                .collect(Collectors.toList());

        List<SupplierOrderItemExport> supplierOrderItems = supplierOrderItemRepo.findByOrderId(order.getId()).stream()
                .map(this::getSupplierOrderItemsForExport)
                .collect(Collectors.toList());

        OrderExportRequest orderExportRequest = new OrderExportRequest();
        orderExportRequest.setProducts(products);
        orderExportRequest.setUsers(userList);
        orderExportRequest.setUserOrder(orderItems);
        orderExportRequest.setSupplierOrder(supplierOrderItems);
        orderExportRequest.setFriends(false);

        try {
            return excelServiceClient.order(orderExportRequest);
        } catch(Exception ex) {
            log.error("Error while calling excel service", ex);
            throw new GoGasException("Error while generating excel file");
        }
    }

    private OrderItemExport getOrderItemsForExport(OrderItem item) {
        OrderItemExport i = new OrderItemExport();
        i.setProductId(item.getProduct());
        i.setUserId(item.getUser());
        i.setQuantity(item.getDeliveredQuantity());
        i.setUnitPrice(item.getPrice());
        return i;
    }

    private List<User> getUsersForExport(List<OrderItemExport> orderItems, boolean exportAllUsers) {
        if (exportAllUsers)
            return userRepo.findAll();

        Set<String> userIdsInOrder = orderItems.stream()
                .map(item -> item.getUserId())
                .collect(Collectors.toSet());

        return userRepo.findByIdIn(userIdsInOrder, User.class);
    }

    private UserExport convertUserForExport(User user) {
        UserExport reportUser = new UserExport();
        reportUser.setId(user.getId());
        reportUser.setFullName(userService.getUserDisplayName(user.getFirstName(), user.getLastName()));
        reportUser.setRole(user.getRole());
        reportUser.setEmail(user.getEmail());
        reportUser.setPhone(user.getPhone());
        return reportUser;
    }

    private List<Product> getProductsForExport(List<OrderItemExport> orderItems, OrderType orderType) {
        if (orderType.isExcelAllProducts())
            return productRepo.findAvailableByTypeOrderByPriceList(orderType.getId());

        Set<String> productIdsInOrder = orderItems.stream()
                .map(item -> item.getProductId())
                .collect(Collectors.toSet());

        return productRepo.findByIdInOrderByPriceList(productIdsInOrder);
    }


    private ProductExport convertProductForExport(Product pr) {
        ProductExport p = new ProductExport();
        p.setId(pr.getId());
        p.setName(pr.getDescription());
        p.setUnitOfMeasure(pr.getUm());
        p.setBoxWeight(pr.getBoxWeight());
        p.setUnitPrice(pr.getPrice());
        return p;
    }

    private SupplierOrderItemExport getSupplierOrderItemsForExport(SupplierOrderItem item) {
        SupplierOrderItemExport s = new SupplierOrderItemExport();
        s.setProductId(item.getProductId());
        s.setUnitPrice(item.getUnitPrice());
        s.setBoxWeight(item.getBoxWeight());
        s.setQuantity(item.getBoxesCount());
        return s;
    }

    public byte[] exportUserTotals(List<UserTotal> userTotals, boolean includeUsers) throws IOException {

        List<ColumnDefinition<UserTotal>> columnDefinitions = Arrays.asList(
                new ColumnDefinition<UserTotal>("Disab.", TextCenter)
                        .withExtract(u -> u.getUser().isEnabled() ? "" : "x"),

                new ColumnDefinition<UserTotal>("Utente", Text)
                        .withExtract(u -> u.getUser().getFirstName() + " " + u.getUser().getLastName()),

                new ColumnDefinition<UserTotal>("Saldo", Currency)
                        .withExtract(u -> u.getTotal().doubleValue())
                        .withShowTotal()
        );

        String title = "Situazione contabile utenti";

        GenericExcelGenerator<UserTotal> userTotalGenericExcelGenerator = new GenericExcelGenerator<>(title, columnDefinitions);
        return userTotalGenericExcelGenerator.generate(userTotals);
    }

    //TODO: handle multiple sheets
    public byte[] exportUserEntries(List<UserTransactionFull> userEntries) throws IOException {

        List<ColumnDefinition<UserTransactionFull>> columnDefinitions = Arrays.asList(
                new ColumnDefinition<UserTransactionFull>("Data", Date)
                        .withExtract(t -> java.util.Date.from(t.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant())),

                new ColumnDefinition<UserTransactionFull>("Descrizione", Text)
                        .withExtract(UserTransactionFull::getDescription),

                new ColumnDefinition<UserTransactionFull>("Accrediti", Currency)
                        .withExtract(t -> t.getAmount().signum() >= 0 ? t.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<UserTransactionFull>("Addebiti", Currency)
                        .withExtract(t -> t.getAmount().signum() < 0 ? t.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<>("Saldo", SubTotal)
        );

        String title = "Situazione contabile gas";

        GenericExcelGenerator<UserTransactionFull> excelGenerator = new GenericExcelGenerator<>(title, columnDefinitions);
        return excelGenerator.generate(userEntries);
    }

    public byte[] exportGasEntries(List<AccountingGasEntryDTO> gasEntries) throws IOException {

        List<ColumnDefinition<AccountingGasEntryDTO>> columnDefinitions = Arrays.asList(
                new ColumnDefinition<AccountingGasEntryDTO>("Data", Date)
                        .withExtract(e -> java.util.Date.from(e.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant())),

                new ColumnDefinition<AccountingGasEntryDTO>("Descrizione", Text)
                        .withExtract(AccountingGasEntryDTO::getDescription),

                new ColumnDefinition<AccountingGasEntryDTO>("Accrediti", Currency)
                        .withExtract(e -> e.getAmount().signum() >= 0 ? e.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<AccountingGasEntryDTO>("Addebiti", Currency)
                        .withExtract(e -> e.getAmount().signum() < 0 ? e.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<>("Saldo", SubTotal)
        );

        String title = "Situazione contabile gas";

        GenericExcelGenerator<AccountingGasEntryDTO> excelGenerator = new GenericExcelGenerator<>(title, columnDefinitions);
        return excelGenerator.generate(gasEntries);
    }

    /*public byte[] exportUserDetails(String userId) throws IOException {
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

            row.createCell(1).setCellValue((t.getReason()==null || t.getReason().isEmpty() ? "" : t.getReason() + " - ") + t.getDescription());

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

    }*/
}
