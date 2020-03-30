package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
public class OrderItemService {

    private OrderItemRepo orderItemRepo;
    private UserOrderSummaryRepo userOrderSummaryRepo;

    public OrderItemService(OrderItemRepo orderItemRepo, UserOrderSummaryRepo userOrderSummaryRepo) {
        this.orderItemRepo = orderItemRepo;
        this.userOrderSummaryRepo = userOrderSummaryRepo;
    }

    public void insertItemByManager(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, true);
        recomputeUserTotal(orderId, user.getId());
    }

    public void insertItemByFriendReferral(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        insertOrUpdateItem(user, product, orderId, orderItemUpdate, Optional.empty(), true, false);
        recomputeUserTotal(orderId, user.getId());
    }

    public OrderItem updateOrDeleteItemByUser(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        Optional<OrderItem> existingOrderItem = orderItemRepo.findByUserAndOrderAndProductAndSummary(user.getId(), orderId, product.getId(), false, OrderItem.class);

        OrderItem modifiedOrderItem = null;
        if (orderItemUpdate.getQuantity() == null || orderItemUpdate.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            existingOrderItem.ifPresent(orderItemRepo::delete);
        else
            modifiedOrderItem = insertOrUpdateItem(user, product, orderId, orderItemUpdate, existingOrderItem, false, false);

        recomputeUserTotal(orderId, user.getId());

        return modifiedOrderItem;
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

    public List<OrderItem> getItemsByOrderAndSummary(String orderId, boolean summary) {
        return orderItemRepo.findByOrderAndSummary(orderId, summary);
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

    public boolean updateDeliveredQty(String orderId, String orderItemId, BigDecimal deliveredQty, boolean recomputeUserTotal) {
        int updatedRows = orderItemRepo.updateDeliveredQtyByItemId(orderId, orderItemId, deliveredQty);

        if (recomputeUserTotal)
            recomputeUserTotalByOrderItem(orderItemId);

        return updatedRows > 0;
    }

    public int updateDeliveredQty(String orderId, String userId, String productId, BigDecimal deliveredQty) {
        return orderItemRepo.updateDeliveredQty(orderId, userId, productId, deliveredQty);
    }

    public Set<String> getUsersWithOrder(String orderId, String productId) {
        return orderItemRepo.findUserOrderingByProductAndSummary(orderId, productId, true);
    }

    public Set<String> getUsersWithNotSummaryOrder(String orderId, String productId) {
        return orderItemRepo.findUserOrderingByProductAndSummary(orderId, productId, false);
    }

    public Set<String> getUsersWithNotSummaryOrder(String orderId) {
        return orderItemRepo.findUserOrderingBySummary(orderId, false);
    }

    public void cancelProductOrder(String orderId, String productId) {
        orderItemRepo.cancelByOrderAndProduct(orderId, productId);
        recomputeAllUsersTotal(orderId);
    }

    public void restoreProductOrder(String orderId, String productId) {
        orderItemRepo.restoreByOrderAndProduct(orderId, productId);
        recomputeAllUsersTotal(orderId);
    }

    public void cancelOrderItem(String orderItemId) {
        orderItemRepo.cancelByOrderItem(orderItemId);
        recomputeUserTotalByOrderItem(orderItemId);
    }

    public void restoreOrderItem(String orderItemId) {
        orderItemRepo.restoreByOrderItem(orderItemId);
        recomputeUserTotalByOrderItem(orderItemId);
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

        recomputeUserTotalByOrderItem(orderItemId);
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
        recomputeAllUsersTotal(orderId);
    }

    public void increaseDeliveredQtyByProduct(String orderId, String product, BigDecimal deliveredQtyRatio) {
        orderItemRepo.increaseDeliveredQtyByProduct(orderId, product, deliveredQtyRatio);
        recomputeAllUsersTotal(orderId);
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

    private void recomputeUserTotalByOrderItem(String orderItemId) {
        OrderItem orderItem = orderItemRepo.findById(orderItemId).get();
        recomputeUserTotal(orderItem.getOrder(), orderItem.getUser());
    }

    private void recomputeUserTotal(String orderId, String userId) {
        UserOrderSummaryExtraction extractedUserOrderSummary = userOrderSummaryRepo.extractUserOrderSummary(orderId, userId);
        UserOrderSummary userOrderSummary = createUserOrderSummary(orderId, extractedUserOrderSummary);

        if (userOrderSummary.getItemsCount() == 0)
            userOrderSummaryRepo.delete(userOrderSummary);
        else
            userOrderSummaryRepo.save(userOrderSummary);
    }

    public void recomputeAllUsersTotal(String orderId) {
        List<UserOrderSummaryExtraction> extractedUserOrderSummaries = userOrderSummaryRepo.extractUserOrderSummaries(orderId);

        List<UserOrderSummary> userOrderSummaries = extractedUserOrderSummaries.stream()
                .map(summary -> createUserOrderSummary(orderId, summary))
                .collect(Collectors.toList());

        userOrderSummaryRepo.deleteAllUserOrderSummary(orderId);
        userOrderSummaryRepo.saveAll(userOrderSummaries);
    }

    private UserOrderSummary createUserOrderSummary(String orderId, UserOrderSummaryExtraction extractedUserOrderSummary) {
        UserOrderSummary userOrderSummary = new UserOrderSummary(orderId, extractedUserOrderSummary.getUserId());
        userOrderSummary.setItemsCount(extractedUserOrderSummary.getItemsCount());
        userOrderSummary.setTotalAmount(extractedUserOrderSummary.getTotalAmount());
        userOrderSummary.setFriendItemsCount(extractedUserOrderSummary.getFriendItemsCount());
        userOrderSummary.setFriendItemsAccounted(extractedUserOrderSummary.getFriendItemsAccounted());
        userOrderSummary.setShippingCost(extractedUserOrderSummary.getShippingCost());
        return userOrderSummary;
    }

    public void saveAll(List<OrderItem> itemsCreated) {
        orderItemRepo.saveAll(itemsCreated);
    }

    public int setCancelledByOrderId(String orderId, boolean cancelled) {
        return orderItemRepo.setCancelledByOrderId(orderId, cancelled);
    }

    public int deleteByOrderAndSummary(String orderId, boolean summary) {
        return orderItemRepo.deleteByOrderAndSummary(orderId, summary);
    }

    public List<OrderItem> findByOrderAndSummary(String orderId, boolean summary) {
        return orderItemRepo.findByOrderAndSummary(orderId, summary);
    }
}
