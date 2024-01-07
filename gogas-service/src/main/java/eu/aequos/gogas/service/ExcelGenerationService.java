package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.AccountingGasEntryDTO;
import eu.aequos.gogas.dto.UserBalanceDTO;
import eu.aequos.gogas.dto.UserBalanceEntryDTO;
import eu.aequos.gogas.dto.UserBalanceSummaryDTO;
import eu.aequos.gogas.excel.ExcelServiceClient;
import eu.aequos.gogas.excel.generic.ColumnDefinition;
import eu.aequos.gogas.excel.generic.ExcelDocumentBuilder;
import eu.aequos.gogas.excel.order.*;
import eu.aequos.gogas.excel.products.ExcelPriceListItem;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.User.Role;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.Integer;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.aequos.gogas.excel.generic.ColumnDefinition.DataType.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class ExcelGenerationService {

    private final ExcelServiceClient excelServiceClient;

    private final UserRepo userRepo;
    private final OrderItemRepo orderItemRepo;
    private final ProductRepo productRepo;
    private final SupplierOrderItemRepo supplierOrderItemRepo;
    private final UserService userService;
    private final AccountingService accountingService;
    private final ConfigurationService configurationService;

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

    public byte[] extractOrderDetails(Order order, boolean requiresWeightColumns) throws ItemNotFoundException, GoGasException {
        List<OrderItem> orderItems = orderItemRepo.findByOrderAndSummary(order.getId(), true);

        List<OrderItemExport> orderItemsForExport = orderItems.stream()
                .map(this::getOrderItemsForExport)
                .collect(Collectors.toList());

        List<User> usersList = getUsersForExport(orderItems, order.getOrderType().isExcelAllUsers(), order.getOrderType().isSummaryRequired());
        List<UserExport> usersExportList = convertUsersForExport(usersList);

        List<ProductExport> products = getProductsForExport(orderItemsForExport, order.getOrderType()).stream()
                .map(this::convertProductForExport)
                .collect(Collectors.toList());

        List<SupplierOrderItemExport> supplierOrderItems = supplierOrderItemRepo.findByOrderId(order.getId()).stream()
                .map(this::getSupplierOrderItemsForExport)
                .collect(Collectors.toList());

        OrderExportRequest orderExportRequest = new OrderExportRequest();
        orderExportRequest.setProducts(products);
        orderExportRequest.setUsers(usersExportList);
        orderExportRequest.setUserOrder(orderItemsForExport);
        orderExportRequest.setSupplierOrder(supplierOrderItems);
        orderExportRequest.setFriends(false);
        orderExportRequest.setAddWeightColumns(requiresWeightColumns);

        try {
            return excelServiceClient.order(orderExportRequest);
        } catch(Exception ex) {
            log.error("Error while calling excel service", ex);
            throw new GoGasException("Error while generating excel file");
        }
    }

    public byte[] extractFriendsOrderDetails(Order order, String userId) throws ItemNotFoundException, GoGasException {
        List<OrderItem> originalOrderItems = orderItemRepo.findByOrderAndUserOrFriend(order.getId(), userId);

        List<User> usersList = getUsersForExport(originalOrderItems, false, false);
        List<UserExport> usersExportList = convertUsersForExport(usersList);

        List<Product> products = getProductsForFriendExport(originalOrderItems);

        List<ProductExport> productsForExport = products.stream()
                .map(this::convertProductForExport)
                .collect(Collectors.toList());

        Map<String, Product> productById = products.stream()
                .collect(ListConverter.toMap(Product::getId));

        List<OrderItemExport> originalOrderItemsForExport = originalOrderItems.stream()
                .map(orderItem -> getOrderItemsForFriendExport(orderItem, productById.get(orderItem.getProduct())))
                .collect(Collectors.toList());

        List<OrderItem> summaryOrderItems = orderItemRepo.findByUserAndOrderAndSummary(userId, order.getId(), true, OrderItem.class);
        List<SupplierOrderItemExport> supplierOrderItems = summaryOrderItems.stream()
                .map(item -> getSupplierOrderItemsForFriendExport(item, productById.get(item.getProduct())))
                .collect(Collectors.toList());

        OrderExportRequest orderExportRequest = new OrderExportRequest();
        orderExportRequest.setProducts(productsForExport);
        orderExportRequest.setUsers(usersExportList);
        orderExportRequest.setUserOrder(originalOrderItemsForExport);
        orderExportRequest.setSupplierOrder(supplierOrderItems);
        orderExportRequest.setFriends(true);

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

    private OrderItemExport getOrderItemsForFriendExport(OrderItem item, Product product) {
        BigDecimal unitQuantity = getOrderedUnitQuantity(item, product);

        OrderItemExport i = new OrderItemExport();
        i.setProductId(item.getProduct());
        i.setUserId(item.getUser());
        i.setQuantity(unitQuantity);
        i.setUnitPrice(item.getPrice());
        return i;
    }

    private BigDecimal getOrderedUnitQuantity(OrderItem item, Product product) {
        if (product == null || product.getBoxWeight() == null) {
            return item.getOrderedQuantity();
        }

        if (!item.getUm().equals(product.getBoxUm())) {
            return item.getOrderedQuantity();
        }

        return item.getOrderedQuantity().multiply(product.getBoxWeight());
    }

    private List<User> getUsersForExport(List<OrderItem> orderItems, boolean exportAllUsers, boolean excludeFriends) {
        if (exportAllUsers) {
            Set<String> roles = excludeFriends ? Set.of(Role.U.name()) : Set.of(Role.U.name(), Role.S.name());
            return userRepo.findByRoleInAndEnabled(roles, true, User.class);
        }

        Set<String> userIdsInOrder = orderItems.stream()
                .map(OrderItem::getUser)
                .collect(Collectors.toSet());

        return userRepo.findByIdIn(userIdsInOrder, User.class);
    }

    private List<UserExport> convertUsersForExport(List<User> users) {
        Comparator<User> userComparator = configurationService.getUserComparatorForOrderExport();

        Function<User, Integer> userPositionBuilder = getUserPositionBuilder();

        return users.stream()
                .sorted(userComparator)
                .map(user -> convertUserForExport(user, userPositionBuilder))
                .collect(Collectors.toList());
    }

    private Function<User, Integer> getUserPositionBuilder() {
        if (configurationService.isUserPositionEnabled()) {
            return User::getPosition;
        }

        AtomicInteger counter = new AtomicInteger();
        return user -> counter.incrementAndGet();
    }

    private UserExport convertUserForExport(User user, Function<User, Integer> userPositionBuilder) {
        UserExport reportUser = new UserExport();
        reportUser.setId(user.getId());
        reportUser.setPosition(userPositionBuilder.apply(user));
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
                .map(OrderItemExport::getProductId)
                .collect(Collectors.toSet());

        return productRepo.findByIdInOrderByPriceList(productIdsInOrder);
    }

    private List<Product> getProductsForFriendExport(List<OrderItem> orderItems) {
        Set<String> productIdsInOrder = orderItems.stream()
                .map(OrderItem::getProduct)
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

    private SupplierOrderItemExport getSupplierOrderItemsForFriendExport(OrderItem item, Product product) {
        BigDecimal boxWeight = Optional.ofNullable(product)
                .map(Product::getBoxWeight)
                .orElse(BigDecimal.ZERO);

        SupplierOrderItemExport s = new SupplierOrderItemExport();
        s.setProductId(item.getProduct());
        s.setUnitPrice(item.getPrice());
        s.setBoxWeight(boxWeight);
        s.setQuantity(item.getDeliveredQuantity());
        return s;
    }

    public byte[] exportUserTotals(boolean includeUserDetails) throws IOException {
        List<UserBalanceDTO> userBalanceList = accountingService.getUserBalanceList();
        return exportTotals(userBalanceList, includeUserDetails);
    }

    public byte[] exportFriendTotals(String friendReferralId, boolean includeUserDetails) throws IOException {
        List<UserBalanceDTO> friendBalanceList = accountingService.getFriendBalanceList(friendReferralId);
        return exportTotals(friendBalanceList, includeUserDetails);
    }

    public byte[] exportTotals(List<UserBalanceDTO> balanceList, boolean includeUserDetails) throws IOException {

        List<UserBalanceDTO> userTotals = balanceList.stream()
                .sorted(Comparator.comparing(UserBalanceDTO::isEnabled).reversed()
                        .thenComparing(UserBalanceDTO::getFullName))
                .collect(Collectors.toList());

        List<ColumnDefinition<UserBalanceDTO>> columnDefinitions = Arrays.asList(
                new ColumnDefinition<UserBalanceDTO>("Disab.", TextCenter)
                        .withExtract(u -> u.isEnabled() ? "" : "x"),

                new ColumnDefinition<UserBalanceDTO>("Utente", Text)
                        .withExtract(UserBalanceDTO::getFullName),

                new ColumnDefinition<UserBalanceDTO>("Saldo", Currency)
                        .withExtract(u -> u.getBalance().doubleValue())
                        .withShowTotal()
        );

        String title = "Situazione contabile utenti";

        ExcelDocumentBuilder excelDocumentBuilder = new ExcelDocumentBuilder()
                .addSheet(title, columnDefinitions, userTotals);

        if (includeUserDetails) {
            for (UserBalanceDTO userTotal : userTotals)
                addUserEntries(excelDocumentBuilder, userTotal.getUserId(), userTotal.getFullName());
        }

        return excelDocumentBuilder.generate();
    }

    public byte[] exportUserEntries(String userId) throws IOException {
        User user = userService.getRequired(userId);

        ExcelDocumentBuilder documentBuilder = new ExcelDocumentBuilder();
        addUserEntries(documentBuilder, user.getId(), userService.getUserDisplayName(user));
        return documentBuilder.generate();
    }

    private void addUserEntries(ExcelDocumentBuilder excelDocumentBuilder, String userId, String userFullName) {

        List<ColumnDefinition<UserBalanceEntryDTO>> columnDefinitions = Arrays.asList(
                new ColumnDefinition<UserBalanceEntryDTO>("Data", Date)
                        .withExtract(t -> java.util.Date.from(t.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant())),

                new ColumnDefinition<UserBalanceEntryDTO>("Descrizione", Text)
                        .withExtract(UserBalanceEntryDTO::getDescription),

                new ColumnDefinition<UserBalanceEntryDTO>("Accrediti", Currency)
                        .withExtract(t -> t.getAmount().signum() >= 0 ? t.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<UserBalanceEntryDTO>("Addebiti", Currency)
                        .withExtract(t -> t.getAmount().signum() < 0 ? t.getAmount().abs().doubleValue() : null)
                        .withShowTotal(),

                new ColumnDefinition<>("Saldo", SubTotal)
        );

        UserBalanceSummaryDTO userBalance = accountingService.getUserBalance(userId, null, null, true);

        List<UserBalanceEntryDTO> entries = userBalance.getEntries().stream()
                .sorted(Comparator.comparing(UserBalanceEntryDTO::getDate))
                .collect(Collectors.toList());

        excelDocumentBuilder.addSheet(userFullName, columnDefinitions, entries);
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

        return new ExcelDocumentBuilder()
                .addSheet(title, columnDefinitions, gasEntries)
                .generate();
    }
}
