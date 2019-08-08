package eu.aequos.gogas.service;

import eu.aequos.gogas.excel.ExcelGeneratorClient;
import eu.aequos.gogas.excel.order.*;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.excel.products.ProductPriceListExport;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExcelGenerationService {

    ExcelGeneratorClient reportClient;

    UserRepo userRepo;
    OrderRepo orderRepo;
    OrderItemRepo orderItemRepo;
    ProductRepo productRepo;
    SupplierOrderItemRepo supplierOrderItemRepo;
    UserService userService;

    public ExcelGenerationService(ExcelGeneratorClient reportClient, UserRepo userRepo, UserService userService,
                                  OrderRepo orderRepo, OrderItemRepo orderItemRepo,
                                  ProductRepo productRepo, SupplierOrderItemRepo supplierOrderItemRepo) {
        this.reportClient = reportClient;
        this.userRepo = userRepo;
        this.userService = userService;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
    }

    public byte[] extractProductPriceList(String orderTypeId) {
        List<ProductPriceListExport> products = productRepo.findByType(orderTypeId).stream()
                .map(this::convertProductForPriceListExport)
                .collect(Collectors.toList());

        return reportClient.products(products);
    }

    private ProductPriceListExport convertProductForPriceListExport(Product p) {
        ProductPriceListExport exp = new ProductPriceListExport();
        exp.setExternalId(p.getExternalId());
        exp.setName(p.getDescription());
        exp.setSupplierExternalId(p.getSupplier().getIdesterno());
        exp.setSupplierName(p.getSupplier().getRagionesociale());
        exp.setSupplierProvince(p.getSupplier().getProvincia());
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

    public byte[] extractOrderDetails(String orderId) throws ItemNotFoundException {
        Order order = orderRepo.findByIdWithType(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order", orderId));

        List<OrderItemExport> orderItems = orderItemRepo.findByOrderAndSummary(orderId, true).stream()
                .map(this::getOrderItemsForExport)
                .collect(Collectors.toList());

        List<UserExport> userList = getUsersForExport(orderItems, order.getOrderType().isExcelAllUsers()).stream()
                .map(this::convertUserForExport)
                .sorted(Comparator.comparing(UserExport::getFullName))
                .collect(Collectors.toList());

        List<ProductExport> products = getProductsForExport(orderItems, order.getOrderType()).stream()
                .map(this::convertProductForExport)
                .collect(Collectors.toList());

        List<SupplierOrderItemExport> supplierOrderItems = supplierOrderItemRepo.findByOrderId(orderId).stream()
                .map(this::getSupplierOrderItemsForExport)
                .collect(Collectors.toList());

        OrderExportRequest orderExportRequest = new OrderExportRequest();
        orderExportRequest.setProducts(products);
        orderExportRequest.setUsers(userList);
        orderExportRequest.setUserOrder(orderItems);
        orderExportRequest.setSupplierOrder(supplierOrderItems);
        orderExportRequest.setFriends(false);

        return reportClient.order(orderExportRequest);
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

}
