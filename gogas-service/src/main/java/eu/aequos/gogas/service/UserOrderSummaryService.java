package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummaryExtraction;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.aequos.gogas.converter.ListConverter.toMap;

@RequiredArgsConstructor
@Service
public class UserOrderSummaryService {

    private final UserOrderSummaryRepo userOrderSummaryRepo;

    public List<UserOrderSummary> findByOrderId(String orderId) {
        return userOrderSummaryRepo.findUserOrderSummaryByOrder(orderId);
    }

    public List<UserOrderSummary> findAggregatedByOrderId(String orderId) {
        return userOrderSummaryRepo.findAggregatedUserOrderSummaryByOrder(orderId);
    }

    public List<UserOrderSummary> findAccountableByOrderId(String orderId) {
        return userOrderSummaryRepo.findAccountableUserOrderSummaryByOrder(orderId);
    }

    public Set<String> getUsersWithOrder(String orderId) {
        return findAggregatedByOrderId(orderId).stream()
                .map(UserOrderSummary::getUserId)
                .collect(Collectors.toSet());
    }

    public List<OrderSummary> getOrdersTotal(Set<String> orderIds) {
        return userOrderSummaryRepo.getOrdersTotal(orderIds);
    }

    public void updateUserTotalForNotComputedOrder(Order order, User user, BigDecimal totalAmount) {
        UserOrderSummary.Key id = new UserOrderSummary.Key(order.getId(), user.getId());
        Optional<UserOrderSummary> existingUserOrderSummary = userOrderSummaryRepo.findById(id);

        if (totalAmount == null) {
            deleteByOrderIdAndUserId(order, user.getId());
            return;
        }

        UserOrderSummary userOrderSummary = existingUserOrderSummary
                .orElseGet(() -> new UserOrderSummary(order.getId(), user.getId()));

        int itemsCount = userOrderSummaryRepo.countUserOrderItemsForNotComputedOrder(order.getId(), user.getId())
                .orElse(0);

        userOrderSummary.setItemsCount(itemsCount);
        userOrderSummary.setTotalAmount(totalAmount);

        if (user.getFriendReferral() != null) {
            userOrderSummary.setFriendReferralId(user.getFriendReferral().getId());
        }

        userOrderSummary.setAggregated(true);

        userOrderSummaryRepo.save(userOrderSummary);
    }

    public void deleteByOrderIdAndUserId(Order order, String userId) {
        boolean external = order.getOrderType().isExternal();

        if (external) {
            userOrderSummaryRepo.deleteByOrderIdAndUserId(order.getId(), userId);
        } else {
            userOrderSummaryRepo.clearByOrderIdAndUserId(order.getId(), userId);
        }
    }

    public void recomputeUserTotalForComputedOrder(String orderId, String userId) {
        Optional<UserOrderSummaryExtraction> extractedUserOrderSummary = userOrderSummaryRepo.extractUserOrderSummary(orderId, userId);

        if (extractedUserOrderSummary.isEmpty()) {
            userOrderSummaryRepo.deleteByOrderIdAndUserId(orderId, userId);
            return;
        }

        UserOrderSummary userOrderSummary = createUserOrderSummary(orderId, extractedUserOrderSummary.get());
        userOrderSummaryRepo.save(userOrderSummary);
    }

    public void recomputeAllUsersTotalForComputedOrder(String orderId) {
        List<UserOrderSummaryExtraction> extractedUserOrderSummaries = userOrderSummaryRepo.extractUserOrderSummaries(orderId);
        updateUserOrderSummaries(orderId, extractedUserOrderSummaries);
    }

    public void recomputeOnOrderClosed(String orderId, List<OrderItem> orderItems, boolean computeAmount) {
        Map<String, List<OrderItem>> itemsByUser = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getUser));

        List<UserOrderSummaryExtraction> extractedUserOrderSummaries = itemsByUser.entrySet().stream()
                .map(entry -> buildUserOrderSummary(computeAmount, entry))
                .collect(Collectors.toList());

        updateUserOrderSummaries(orderId, extractedUserOrderSummaries);
    }

    private InMemoryUserOrderSummaryExtraction buildUserOrderSummary(boolean computeAmount, Entry<String, List<OrderItem>> entry) {
        String userId = entry.getKey();
        List<OrderItem> orderItems = entry.getValue();

        return InMemoryUserOrderSummaryExtraction.builder()
                .userId(userId)
                .friendReferralId(orderItems.get(0).getFriendReferral())
                .itemsCount(orderItems.size())
                .totalAmount(computeAmount(computeAmount, orderItems))
                .build();
    }

    private BigDecimal computeAmount(boolean computeAmount, List<OrderItem> orderItems) {
        if (!computeAmount) {
            return null;
        }

        return orderItems.stream()
                    .map(item -> item.getDeliveredQuantity().multiply(item.getPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
    }

    public void recomputeOnOrderReopened(String orderId) {
        userOrderSummaryRepo.deleteAllUserOrderSummary(orderId);

        List<UserOrderSummaryExtraction> extractedUserOrderSummaries = userOrderSummaryRepo.extractUserOrderSummaries(orderId);
        updateUserOrderSummaries(orderId, extractedUserOrderSummaries);
    }

    private void updateUserOrderSummaries(String orderId, List<UserOrderSummaryExtraction> extractedUserOrderSummaries) {

        Map<String, UserOrderSummary> existingUserSummary = userOrderSummaryRepo.findUserOrderSummaryByOrder(orderId).stream()
                .collect(toMap(UserOrderSummary::getUserId));

        List<UserOrderSummary> userOrderSummaries = extractedUserOrderSummaries.stream()
                .map(summary -> createOrUpdateUserOrderSummary(orderId, summary, existingUserSummary.get(summary.getUserId())))
                .collect(Collectors.toList());

        userOrderSummaryRepo.saveAll(userOrderSummaries);
    }

    private UserOrderSummary createOrUpdateUserOrderSummary(String orderId, UserOrderSummaryExtraction extractedSummary,
                                                            UserOrderSummary existingSummary) {

        if (existingSummary == null) {
            return createUserOrderSummary(orderId, extractedSummary);
        }

        existingSummary.setAggregated(extractedSummary.isAggregated());
        existingSummary.setTotalAmount(extractedSummary.getTotalAmount());
        return existingSummary;
    }

    private UserOrderSummary createUserOrderSummary(String orderId, UserOrderSummaryExtraction extractedUserOrderSummary) {
        UserOrderSummary userOrderSummary = new UserOrderSummary(orderId, extractedUserOrderSummary.getUserId());
        userOrderSummary.setFriendReferralId(extractedUserOrderSummary.getFriendReferralId());
        userOrderSummary.setItemsCount(extractedUserOrderSummary.getItemsCount());
        userOrderSummary.setAggregated(extractedUserOrderSummary.isAggregated());
        userOrderSummary.setTotalAmount(extractedUserOrderSummary.getTotalAmount());
        return userOrderSummary;
    }

    //for testing purposes only
    public void clear() {
        userOrderSummaryRepo.deleteAll();
    }

    //for testing purposes only
    public void clear(String orderId) {
        userOrderSummaryRepo.deleteAllUserOrderSummary(orderId);
    }

    @Builder
    @Getter
    private static class InMemoryUserOrderSummaryExtraction implements UserOrderSummaryExtraction {
        private String userId;
        private String friendReferralId;
        private int itemsCount;
        private BigDecimal totalAmount;

        @Override
        public int getAccountedItemsCount() {
            return 0;
        }

        @Override
        public boolean isAggregated() {
            return true;
        }
    }
}
