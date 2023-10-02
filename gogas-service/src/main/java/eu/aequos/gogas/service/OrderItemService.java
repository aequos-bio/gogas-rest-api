package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

@Service
public class OrderItemService {

    private OrderItemRepo orderItemRepo;

    public OrderItemService(OrderItemRepo orderItemRepo) {
        this.orderItemRepo = orderItemRepo;
    }

    public OrderItem insertItemByManager(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        orderItemRepo.findByUserAndOrderAndProductAndSummary(user.getId(), orderId, product.getId(), true, OrderItem.class)
                .ifPresent(item -> { throw new GoGasException("User item already existing for order and product"); });

        return insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, true);
    }

    public OrderItem insertItemByFriendReferral(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        return insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, false);
    }

    public OrderItemUpdate updateOrDeleteItemByUser(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        Optional<OrderItem> existingOrderItem = orderItemRepo.findByUserAndOrderAndProductAndSummary(user.getId(), orderId, product.getId(), false, OrderItem.class);

        if (orderItemUpdate.getQuantity() == null || orderItemUpdate.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            existingOrderItem.ifPresent(orderItemRepo::delete);
            return new OrderItemUpdate(null, -1);
        }

        OrderItem orderItem = insertOrUpdateItem(user, product, orderId, orderItemUpdate, existingOrderItem, false, false);
        return new OrderItemUpdate(orderItem, existingOrderItem.isPresent() ? 0 : 1);
    }

    private OrderItem insertOrUpdateItem(User user, Product product, String orderId,
                                         OrderItemUpdateRequest orderItemUpdate,
                                         Optional<OrderItem> existingOrderItem,
                                         boolean orderClosed, boolean isSummaryOrder) {

        OrderItem orderItem = existingOrderItem.orElse(newOrderItem(orderId, user, product.getId(), isSummaryOrder));
        orderItem.setUm(orderItemUpdate.getUnitOfMeasure());
        orderItem.setPrice(product.getPrice());

        if (orderClosed) {
            orderItem.setDeliveredQuantity(orderItemUpdate.getQuantity());
            orderItem.setOrderedQuantity(BigDecimal.ZERO);
        } else
            orderItem.setOrderedQuantity(orderItemUpdate.getQuantity());

        return orderItemRepo.save(orderItem);
    }

    private OrderItem newOrderItem(String orderId, User user, String productId, boolean summary) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(orderId);
        orderItem.setUser(user.getId());
        orderItem.setProduct(productId);
        orderItem.setSummary(summary);

        if (user.getRoleEnum().isFriend())
            orderItem.setFriendReferral(user.getFriendReferral().getId());

        return orderItem;
    }

    public Map<String, OpenOrderItem> getUserOrderItems(String userId, String orderId, boolean showGroupedOrderItems) {
        return orderItemRepo.findByUserAndOrderAndSummary(userId, orderId, showGroupedOrderItems, OpenOrderItem.class).stream()
                .collect(toMap(OpenOrderItem::getProduct, Function.identity()));
    }

    public Optional<ProductTotalOrder> getTotalQuantityByProduct(String orderId, String productId) {
        return orderItemRepo.totalQuantityAndUsersByProductForOpenOrder(orderId, productId);
    }

    public List<ProductTotalOrder> getTotalQuantityByProduct(String orderId, boolean isOrderOpen) {
        if (isOrderOpen)
            return orderItemRepo.totalQuantityAndUsersByProductForOpenOrder(orderId);
        else
            return orderItemRepo.totalQuantityAndUsersByProductForClosedOrder(orderId);
    }

    public Map<String, FriendTotalOrder> getFriendTotalQuantity(String userId, String orederId) {
        return orderItemRepo.totalQuantityNotSummaryByUserOrReferral(userId, orederId).stream()
                .collect(toMap(FriendTotalOrder::getProduct, Function.identity()));
    }

    public List<ByUserOrderItem> getItemsCountAndAmountByUser(String orderId, boolean isClosed) {
        return orderItemRepo.itemsCountAndAmountByUserForOrder(orderId, isClosed);
    }

    public List<ByProductOrderItem> getItemsByProduct(String productId, String orderId, boolean summary) {
        return orderItemRepo.findByProductAndOrderAndSummary(productId, orderId, summary, ByProductOrderItem.class);
    }

    public List<ByProductOrderItem> getFriendItemsByProduct(String userId, String productId, String orderId) {
        return orderItemRepo.findNotSummaryByUserOrReferral(userId, orderId, productId, ByProductOrderItem.class);
    }

    public Optional<OrderItem> getSummaryUserItemByProduct(String userId, String productId, String orderId) {
        return orderItemRepo.findByUserAndOrderAndProductAndSummary(userId, orderId, productId, true, OrderItem.class);
    }

    public List<OrderItemQtyOnly> getFriendQuantitiesByProduct(String userId, String productId, String orderId) {
        return orderItemRepo.findNotSummaryByUserOrReferral(userId, orderId, productId, OrderItemQtyOnly.class);
    }

    public Optional<OrderItemQtyOnly> getSummaryUserQuantityByProduct(String userId, String productId, String orderId) {
        return orderItemRepo.findByUserAndOrderAndProductAndSummary(userId, orderId, productId, true, OrderItemQtyOnly.class);
    }

    public boolean updateDeliveredQty(String orderId, String orderItemId, BigDecimal deliveredQty) {
        int updatedRows = orderItemRepo.updateDeliveredQtyByItemId(orderId, orderItemId, deliveredQty);
        return updatedRows > 0;
    }

    public Set<String> getUsersWithOrder(String orderId, String productId) {
        return orderItemRepo.findUserOrderingByProductAndSummary(orderId, productId, true);
    }

    public Set<String> getUsersWithOrder(String orderId) {
        return orderItemRepo.findUserOrderingBySummary(orderId, true);
    }

    public Set<String> getUsersWithNotSummaryOrder(String orderId, String productId) {
        return orderItemRepo.findUserOrderingByProductAndSummary(orderId, productId, false);
    }

    public Set<String> getUsersWithNotSummaryOrder(String orderId) {
        return orderItemRepo.findUserOrderingBySummary(orderId, false);
    }

    public void restoreProductOrder(String orderId, String productId) {
        orderItemRepo.restoreByOrderAndProduct(orderId, productId);
    }

    public int cancelOrderItemByOrderAndProduct(String orderId, String productId) {
        return orderItemRepo.cancelByOrderAndProduct(orderId, productId);
    }

    public void cancelOrderItem(String orderItemId, String orderId) {
        int updatedRows = orderItemRepo.cancelByOrderItem(orderItemId, orderId);

        if (updatedRows == 0) {
            throw new ItemNotFoundException("orderItem", orderItemId);
        }
    }

    public void restoreOrderItem(String orderItemId) {
        orderItemRepo.restoreByOrderItem(orderItemId);
    }

    @Transactional
    public void replaceOrderItemsProduct(String orderItemId, String orderId, boolean summaryRequired,
                                         String targetProduct, SupplierOrderItem supplierOrderItem) throws ItemNotFoundException {

        OrderItem selectedOrderItem = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ItemNotFoundException("orderItem", orderItemId));

        if (!selectedOrderItem.getOrder().equalsIgnoreCase(orderId)) {
            throw new ItemNotFoundException("orderItem", orderItemId);
        }

        if (targetProduct.equalsIgnoreCase(selectedOrderItem.getProduct())) {
            throw new GoGasException("Cannot replace a product with the same product");
        }

        int updated = orderItemRepo.addDeliveredQtyToOrderItem(selectedOrderItem.getUser(), selectedOrderItem.getOrder(),
                                                                targetProduct, selectedOrderItem.getDeliveredQuantity());

        if (updated == 0) {
            List<OrderItem> originalOrderItems = new ArrayList<>(getOriginalOrders(selectedOrderItem.getUser(), selectedOrderItem.getOrder(),
                    selectedOrderItem.getProduct(), summaryRequired));
            originalOrderItems.add(selectedOrderItem);

            List<OrderItem> clonedOrderItems = originalOrderItems.stream()
                    .map(o -> cloneForProductReplacement(o, targetProduct, supplierOrderItem))
                    .collect(toList());

            orderItemRepo.saveAll(clonedOrderItems);
        }

        cancelOrderItem(orderItemId, orderId);
    }

    private OrderItem cloneForProductReplacement(OrderItem original, String targetProduct, SupplierOrderItem supplierOrderItem) {
        OrderItem clone = new OrderItem();
        clone.setUser(original.getUser());
        clone.setOrder(original.getOrder());
        clone.setProduct(targetProduct);
        clone.setReplacedProduct(original.getProduct());
        clone.setPrice(supplierOrderItem.getUnitPrice());
        clone.setOrderedQuantity(BigDecimal.ZERO);
        clone.setUm(original.getUm());
        clone.setDeliveredQuantity(original.getDeliveredQuantity());
        clone.setSummary(original.isSummary());
        clone.setCancelled(original.isCancelled());
        clone.setAccounted(original.isAccounted());
        clone.setFriendReferral(original.getFriendReferral());
        return clone;
    }

    private List<OrderItem> getOriginalOrders(String user, String order,
                                              String product, boolean summaryRequired) {

        if (summaryRequired)
            return orderItemRepo.findNotSummaryByUserOrReferral(user, order, product, OrderItem.class);

        return orderItemRepo.findByUserAndOrderAndProductAndSummary(user, order, product, false, OrderItem.class)
                .map(Arrays::asList)
                .orElse(new ArrayList<>());
    }

    public void updatePriceByOrderIdAndProductId(String orderId, String productId, BigDecimal price) {
        orderItemRepo.updatePriceByOrderIdAndProductId(orderId, productId, price);
    }

    public void increaseDeliveredQtyByProduct(String orderId, String product, BigDecimal deliveredQtyRatio) {
        orderItemRepo.increaseDeliveredQtyByProduct(orderId, product, deliveredQtyRatio);
    }

    public void accountFriendOrder(String userId, String orderId, String productId, boolean accounted) {
        orderItemRepo.updateAccountedByOrderIdAndProductIdAndUserOrFriend(userId, orderId, productId, accounted);
    }

    public boolean isOrderItemBelongingToUserOrFriend(String orderItem, String userId) {
        return orderItemRepo.findOrderItemByIdAndUserOrFriend(orderItem, userId).isPresent();
    }

    public Map<String, List<String>> getBuyersInOrderIds(Set<String> orderIds) {
        return orderItemRepo.findDistinctByOrderIn(orderIds).stream()
                .collect(groupingBy(OrderItemUserOnly::getUser, mapping(OrderItemUserOnly::getOrder, toList())));
    }

    public List<Product> getNotOrderedProductsByCategory(String userId, String orderId, String categoryId) {
        return orderItemRepo.getNotOrderedProductsByCategory(userId, orderId, categoryId);
    }

    @Getter
    @RequiredArgsConstructor
    public static class OrderItemUpdate {
        private final OrderItem orderItem;
        private final int itemsAdded;
    }
}
