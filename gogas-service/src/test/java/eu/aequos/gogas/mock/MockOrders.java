package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@WithTenant("integration-test")
public class MockOrders {

    private final OrderTypeRepo orderTypeRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final ProductCategoryRepo productCategoryRepo;
    private final SupplierRepo supplierRepo;
    private final OrderManagerRepo orderManagerRepo;

    public OrderType createAequosOrderType(String name, Integer aequosId) {
        OrderType orderType = new OrderType();
        orderType.setDescription(name);
        orderType.setAequosOrderId(aequosId);
        return orderTypeRepo.save(orderType);
    }

    public OrderType createOrderType(String name) {
        OrderType orderType = new OrderType();
        orderType.setDescription(name);
        return orderTypeRepo.save(orderType);
    }

    public ProductCategory createCategory(String name, String orderTypeId) {
        ProductCategory category = new ProductCategory();
        category.setDescription(name);
        category.setOrderTypeId(orderTypeId);
        return productCategoryRepo.save(category);
    }

    public Supplier createSupplier(String id, String name) {
        Supplier supplier = new Supplier();
        supplier.setExternalId(id);
        supplier.setName(name);
        return supplierRepo.save(supplier);
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

    public Order createOrder(String orderTypeId) {
        OrderType orderType = new OrderType();
        orderType.setId(orderTypeId);
        return createExistingOrder(orderType);
    }

    public Order createExistingOrder(OrderType orderType) {
        Order order = new Order();
        order.setOrderType(orderType);
        order.setStatusCode(Order.OrderStatus.Opened.getStatusCode());
        order.setOpeningDate(LocalDate.now().minusDays(2));
        order.setDueDate(LocalDate.now().plusDays(1));
        order.setDeliveryDate(LocalDate.now().plusDays(2));
        order.setShippingCost(BigDecimal.ZERO);
        return orderRepo.save(order);
    }

    public void deleteOrder(String orderId) {
        orderItemRepo.deleteByOrderAndSummary(orderId, false);
        orderItemRepo.deleteByOrderAndSummary(orderId, true);
        orderRepo.deleteById(orderId);
    }

    public void deleteAllOrderTypes() {
        orderItemRepo.deleteAll();
        orderRepo.deleteAll();
        productRepo.deleteAll();
        supplierRepo.deleteAll();
        productCategoryRepo.deleteAll();
        orderManagerRepo.deleteAll();
        orderTypeRepo.deleteAll();
    }

    public void destroy() {
        deleteAllOrderTypes();
    }
}
