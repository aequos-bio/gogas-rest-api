package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.service.UserOrderSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@WithTenant("integration-test")
public class MockOrdersData implements MockDataLifeCycle {

    private final OrderTypeRepo orderTypeRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final ProductCategoryRepo productCategoryRepo;
    private final SupplierRepo supplierRepo;
    private final OrderManagerRepo orderManagerRepo;
    private final SupplierOrderItemRepo supplierOrderItemRepo;
    private final AccountingRepo accountingRepo;
    private final ShippingCostRepo shippingCostRepo;
    private final UserOrderSummaryService userOrderSummaryService;

    public OrderType createAequosOrderType(String name, Integer aequosId) {
        OrderType orderType = new OrderType();
        orderType.setDescription(name);
        orderType.setAequosOrderId(aequosId);
        orderType.setBilledByAequos(true);
        return orderTypeRepo.save(orderType);
    }

    public OrderType createOrderType(String name) {
        return createOrderType(name, false, false);
    }

    public OrderType createExternalOrderType(String name) {
        return createOrderType(name, false, true);
    }

    public OrderType createOrderType(String name, boolean computedAmount) {
        return createOrderType(name, computedAmount, false);
    }

    private OrderType createOrderType(String name, boolean computedAmount, boolean external) {
        OrderType orderType = new OrderType();
        orderType.setDescription(name);
        orderType.setComputedAmount(computedAmount);
        orderType.setExternal(external);
        return orderTypeRepo.save(orderType);
    }

    public void forceSummaryRequired(OrderType orderType, boolean summaryRequired) {
        orderType.setSummaryRequired(summaryRequired);
        orderTypeRepo.save(orderType);
    }

    public void forceShowAdvance(OrderType orderType, boolean showAdvance) {
        orderType.setShowAdvance(showAdvance);
        orderTypeRepo.save(orderType);
    }

    public void forceShowBoxCompletion(OrderType orderType, boolean showBoxCompletion) {
        orderType.setShowBoxCompletion(showBoxCompletion);
        orderTypeRepo.save(orderType);
    }

    public ProductCategory createCategory(String name, String orderTypeId) {
        return createCategory(name, orderTypeId, 0, null);
    }

    public ProductCategory createCategory(String name, String orderTypeId, int position) {
        return createCategory(name, orderTypeId, position, null);
    }

    public ProductCategory createCategory(String name, String orderTypeId, int position, String color) {
        ProductCategory category = new ProductCategory();
        category.setDescription(name);
        category.setOrderTypeId(orderTypeId);
        category.setPriceListPosition(position);
        category.setPriceListColor(color);
        return productCategoryRepo.save(category);
    }

    public Supplier createSupplier(String id, String name) {
        return createSupplier(id, name, null);
    }

    public Supplier createSupplier(String id, String name, String prov) {
        Supplier supplier = new Supplier();
        supplier.setExternalId(id);
        supplier.setName(name);
        supplier.setProvince(prov);
        return supplierRepo.save(supplier);
    }

    public Product createProduct(String orderTypeId, String externalId,
                                 String description, Supplier supplier, ProductCategory category,
                                 String um, String boxUm, Double boxWeight, Double price) {

        return createProduct(orderTypeId, externalId, description, supplier, category, true,
                false, false, um, boxUm, boxWeight, price, null, null, null);
    }

    public Product createProduct(String orderTypeId, String externalId,
                                  String description, Supplier supplier, ProductCategory category,
                                  boolean available, boolean cancelled, boolean boxOnly, String um, String boxUm,
                                  Double boxWeight, Double price, Double multiple, String notes, String frequency) {

        Product product = new Product();
        product.setType(orderTypeId);
        product.setExternalId(externalId);
        product.setFrequency(frequency);
        product.setDescription(description);
        product.setSupplier(supplier);
        product.setCategory(category);
        product.setAvailable(available);
        product.setCancelled(cancelled);
        product.setBoxOnly(boxOnly);
        product.setUm(um);
        product.setBoxUm(boxUm);
        product.setPrice(BigDecimal.valueOf(price));
        product.setBoxWeight(BigDecimal.valueOf(boxWeight));
        product.setMultiple(Optional.ofNullable(multiple).map(BigDecimal::valueOf).orElse(null));
        product.setNotes(notes);

        return productRepo.save(product);
    }

    public void addManager(User user, OrderType orderType) {
        OrderManager orderManager = new OrderManager();
        orderManager.setOrderType(orderType.getId());
        orderManager.setUser(user.getId());
        orderManagerRepo.save(orderManager);
    }

    public Order createOpenOrder(OrderType orderType) {
        return createOrder(orderType, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Order.OrderStatus.Opened.getStatusCode(), BigDecimal.ZERO);
    }

    public Order createOrder(OrderType orderType, String openingDate, String dueDate, String deliveryDate, Order.OrderStatus orderStatus) {
        return createOrder(orderType, LocalDate.parse(openingDate), LocalDate.parse(dueDate), LocalDate.parse(deliveryDate),
                orderStatus.getStatusCode(), BigDecimal.ZERO);
    }

    public Order createOrder(OrderType orderType, LocalDate openingDate, LocalDate dueDate, LocalDate deliveryDate,
                             int status, BigDecimal shippingCost) {

        return createOrder(orderType, openingDate, dueDate, deliveryDate, status, shippingCost, null);
    }

    public Order createOrder(OrderType orderType, LocalDate openingDate, LocalDate dueDate, LocalDate deliveryDate,
                             int status, BigDecimal shippingCost, String externalLink) {

        Order order = new Order();
        order.setOrderType(orderType);
        order.setStatusCode(status);
        order.setOpeningDate(openingDate);
        order.setDueDate(dueDate);
        order.setDeliveryDate(deliveryDate);
        order.setShippingCost(shippingCost);
        order.setExternaLlink(externalLink);
        return orderRepo.save(order);
    }

    public void forceOrderDates(String orderId, LocalDate openingDate, LocalDateTime dueDateTime, LocalDate deliveryDate) {
        Order order = orderRepo.findById(orderId).get();
        order.setOpeningDate(openingDate);
        order.setDueDate(dueDateTime.toLocalDate());
        order.setDueHour(dueDateTime.getHour());
        order.setDeliveryDate(deliveryDate);
        orderRepo.save(order);
    }

    public void updateExternalOrderId(Order order, String externalOrderId) {
        order.setExternalOrderId(externalOrderId);
        order.setSent(true);
        orderRepo.save(order);
    }

    public SupplierOrderItem createSupplierOrderItem(Order order, Product product, int boxCount) {

        SupplierOrderItem orderItem = new SupplierOrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(product.getId());
        orderItem.setBoxesCount(BigDecimal.valueOf(boxCount));
        orderItem.setBoxWeight(product.getBoxWeight());
        orderItem.setTotalQuantity(BigDecimal.valueOf(boxCount).multiply(product.getBoxWeight()));
        orderItem.setProductExternalCode(product.getExternalId());
        orderItem.setUnitPrice(product.getPrice());

        return supplierOrderItemRepo.save(orderItem);
    }

    public void createDeliveredUserOrderItem(String orderId, String userId, Product product, double quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(orderId);
        orderItem.setUser(userId);
        orderItem.setProduct(product.getId());
        orderItem.setOrderedQuantity(BigDecimal.valueOf(quantity));
        orderItem.setDeliveredQuantity(BigDecimal.valueOf(quantity));
        orderItem.setPrice(product.getPrice());
        orderItem.setUm(product.getUm());
        orderItem.setSummary(true);
        orderItemRepo.save(orderItem);
    }

    public void updateUserTotals(String orderId) {
        userOrderSummaryService.recomputeAllUsersTotalForComputedOrder(orderId);
    }

    @Transactional
    public void deleteOrder(String orderId) {
        orderItemRepo.deleteByOrderAndSummary(orderId, false);
        orderItemRepo.deleteByOrderAndSummary(orderId, true);
        userOrderSummaryService.clear(orderId);
        supplierOrderItemRepo.deleteByOrderId(orderId);
        accountingRepo.deleteAll(accountingRepo.findByOrderId(orderId));
        shippingCostRepo.deleteAll(shippingCostRepo.findByOrderId(orderId));
        orderRepo.deleteById(orderId);
    }

    public void deleteAllOrderTypes() {
        orderItemRepo.deleteAll();
        userOrderSummaryService.clear();
        supplierOrderItemRepo.deleteAll();
        accountingRepo.deleteAll();
        shippingCostRepo.deleteAll();
        orderRepo.deleteAll();
        productRepo.deleteAll();
        supplierRepo.deleteAll();
        productCategoryRepo.deleteAll();
        orderManagerRepo.deleteAll();
        orderTypeRepo.deleteAll();
    }

    public void deleteProduct(String productId) {
        productRepo.deleteById(productId);
    }

    @Override
    public void init() {}

    @Override
    public void destroy() {
        deleteAllOrderTypes();
    }
}
