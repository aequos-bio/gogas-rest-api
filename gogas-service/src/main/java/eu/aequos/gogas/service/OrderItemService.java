package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
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
        return insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, true);
    }

    public OrderItem insertItemByFriendReferral(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        return insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, false);
    }

    public OrderItem updateOrDeleteItemByUser(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        Optional<OrderItem> existingOrderItem = orderItemRepo.findByUserAndOrderAndProductAndSummary(user.getId(), orderId, product.getId(), false, OrderItem.class);

        if (orderItemUpdate.getQuantity() == null || orderItemUpdate.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            existingOrderItem.ifPresent(orderItemRepo::delete);
            return null;
        }

        return insertOrUpdateItem(user, product, orderId, orderItemUpdate, existingOrderItem, false, false);
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

    public List<ByUserOrderItem> getItemsCountAndAmountByUser(String orderId) {
        return orderItemRepo.itemsCountAndAmountByUserForClosedOrder(orderId);
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

    public void cancelProductOrder(String orderId, String productId) {
        orderItemRepo.cancelByOrderAndProduct(orderId, productId);
    }

    public void restoreProductOrder(String orderId, String productId) {
        orderItemRepo.restoreByOrderAndProduct(orderId, productId);
    }

    public void cancelOrderItem(String orderItemId) {
        orderItemRepo.cancelByOrderItem(orderItemId);
    }

    public void restoreOrderItem(String orderItemId) {
        orderItemRepo.restoreByOrderItem(orderItemId);
    }

    @Transactional
    public void replaceOrderItemsProduct(String orderItemId, boolean summaryRequired,
                                         String targetProduct, SupplierOrderItem supplierOrderItem) throws ItemNotFoundException {

        OrderItem selectedOrder = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ItemNotFoundException("orderItem", orderItemId));

        int updated = orderItemRepo.addDeliveredQtyToOrderItem(selectedOrder.getUser(), selectedOrder.getOrder(),
                                                                targetProduct, selectedOrder.getDeliveredQuantity());

        if (updated == 0) {
            List<OrderItem> originalOrderItems = new ArrayList<>();
            originalOrderItems.addAll(getOriginalOrders(selectedOrder.getUser(), selectedOrder.getOrder(),
                    selectedOrder.getProduct(), summaryRequired));
            originalOrderItems.add(selectedOrder);

            List<OrderItem> clonedOrderItems = originalOrderItems.stream()
                    .map(o -> cloneForProductReplacement(o, targetProduct, supplierOrderItem))
                    .collect(toList());

            orderItemRepo.saveAll(clonedOrderItems);
        }

        cancelOrderItem(orderItemId);
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
}
