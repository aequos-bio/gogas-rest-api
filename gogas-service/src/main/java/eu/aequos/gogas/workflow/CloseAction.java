package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class CloseAction extends OrderStatusAction {

    private ConfigurationService.RoundingMode roundingMode;
    private ConfigurationService configurationService;
    private ProductRepo productRepo;

    private Map<String, Product> productMap;

    public CloseAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                       SupplierOrderItemRepo supplierOrderItemRepo,
                       ConfigurationService.RoundingMode roundingMode,
                       Order order, ProductRepo productRepo,
                       ConfigurationService configurationService) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Closed);
        this.roundingMode = roundingMode;
        this.configurationService = configurationService;
        this.productRepo = productRepo;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (!order.getStatus().isOpen())
            return notValid("Invalid order status");

        return valid();
    }

    @Override
    protected void processOrder() {
        List<OrderItem> savedOrderItems = processOrderItems();
        orderItemRepo.saveAll(savedOrderItems);

        List<SupplierOrderItem> supplierOrderItems = processSupplierOrders(savedOrderItems);
        supplierOrderItemRepo.saveAll(supplierOrderItems);
    }

    private List<OrderItem> processOrderItems() {
        List<OrderItem> source = orderItemRepo.findByOrderAndSummary(order.getId(), false);

        List<OrderItem> normalizedOrderItems = copyOrderItemsWithNormalizedQty(source);

        if (order.getOrderType().isSummaryRequired())
            return groupOrderItemsByUser(normalizedOrderItems);

        return normalizedOrderItems;
    }

    private List<OrderItem> copyOrderItemsWithNormalizedQty(List<OrderItem> source) {

        productMap = productRepo.findByIdIn(getProductIds(source)).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return source.stream()
                .map(item -> {
                    Product itemProduct = productMap.get(item.getProduct());
                    BigDecimal qty = getUnitQuantity(itemProduct, item.getOrderedQuantity(), item.getUm());

                    OrderItem groupedItem = new OrderItem();
                    groupedItem.setOrder(item.getOrder());
                    groupedItem.setProduct(item.getProduct());
                    groupedItem.setUser(item.getUser());
                    groupedItem.setFriendReferral(item.getFriendReferral());
                    groupedItem.setUm(itemProduct.getUm());
                    groupedItem.setOrderedQuantity(qty);
                    groupedItem.setDeliveredQuantity(qty);
                    groupedItem.setPrice(itemProduct.getPrice());
                    groupedItem.setSummary(true);
                    groupedItem.setCancelled(false);
                    groupedItem.setAccounted(false);
                    return groupedItem;
                })
                .collect(Collectors.toList());
    }

    private Set<String> getProductIds(List<OrderItem> source) {
        return source.stream()
                .map(OrderItem::getProduct)
                .collect(Collectors.toSet());
    }

    private BigDecimal getUnitQuantity(Product product, BigDecimal orderedQty, String orderedUnit) {
        if (orderedUnit.equalsIgnoreCase(product.getUm()))
            return orderedQty;

        return orderedQty.multiply(product.getBoxWeight());
    }

    private List<OrderItem> groupOrderItemsByUser(List<OrderItem> normalizedOrderItems) {
        Map<OrderItemsKey, List<OrderItem>> groupedOrderItems = normalizedOrderItems.stream()
                .collect(Collectors.groupingBy(o -> new OrderItemsKey(o.getProduct(), getUniqueUser(o))));


        return groupedOrderItems.entrySet().stream()
                .map(userMap -> groupOrderItems(userMap.getKey().getUser(), userMap.getValue()))
                .collect(Collectors.toList());
    }

    private String getUniqueUser(OrderItem orderItem) {
        return Optional.ofNullable(orderItem.getFriendReferral())
                .orElse(orderItem.getUser());
    }

    private OrderItem groupOrderItems(String user, List<OrderItem> orderItems) {
        BigDecimal totalQty = orderItems.stream()
                .map(OrderItem::getOrderedQuantity)
                .reduce(BigDecimal::add)
                .get();

        OrderItem groupedItem = orderItems.stream().findFirst().get();
        groupedItem.setUser(user);
        groupedItem.setOrderedQuantity(totalQty);
        groupedItem.setDeliveredQuantity(totalQty);
        return groupedItem;
    }

    private List<SupplierOrderItem> processSupplierOrders(List<OrderItem> orderItems) {
        Map<String, List<OrderItem>> orderItemsByProduct = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProduct));

        return orderItemsByProduct.entrySet().stream()
                .map(entry -> createSupplierOrderItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private SupplierOrderItem createSupplierOrderItem(String productId, List<OrderItem> orderItems) {
        Product product = productMap.get(productId);

        BigDecimal boxesCount = getBoxesCount(sum(orderItems), product.getBoxWeight());

        SupplierOrderItem supplierOrderItem = new SupplierOrderItem();
        supplierOrderItem.setOrderId(order.getId());
        supplierOrderItem.setProductId(product.getId());
        supplierOrderItem.setProductExternalCode(product.getExternalId());
        supplierOrderItem.setUnitPrice(product.getPrice());
        supplierOrderItem.setBoxWeight(product.getBoxWeight());
        supplierOrderItem.setBoxesCount(boxesCount);
        supplierOrderItem.setTotalQuantity(boxesCount.multiply(product.getBoxWeight()));

        return supplierOrderItem;
    }

    private BigDecimal getBoxesCount(BigDecimal totalOrderedQty, BigDecimal boxWeight) {
        BigDecimal boxesCount = totalOrderedQty.divide(boxWeight, RoundingMode.HALF_UP);

        switch (roundingMode) {
            case Ceil:
                return boxesCount.setScale(0, RoundingMode.CEILING);

            case Floor:
                return boxesCount.setScale(0, RoundingMode.FLOOR);

            case Threshold:
                BigDecimal intPart = boxesCount.setScale(0, RoundingMode.FLOOR);
                BigDecimal decimalPart = boxesCount.remainder(BigDecimal.ONE);

                if (decimalPart.compareTo(configurationService.getBoxRoundingThreshold()) >= 0)
                    intPart.add(BigDecimal.ONE);

                return intPart;

            default:
                return boxesCount;
        }
    }

    private BigDecimal sum(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getDeliveredQuantity)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    @Value
    private static final class OrderItemsKey {
        private final String product;
        private final String user;
    }
}
