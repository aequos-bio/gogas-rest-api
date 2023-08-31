package eu.aequos.gogas.order.user;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.order.OrderBaseIntegrationTest;
import eu.aequos.gogas.persistence.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendOrderIntegrationTest extends OrderBaseIntegrationTest {

    private Order computedOrder;

    @BeforeEach
    void createOrders() {
        computedOrder = mockOrdersData.createOpenOrder(orderTypeComputed);
        createdOrderIds.add(computedOrder.getId());
    }

    @AfterEach
    void clearUserBalance() {
        mockAccountingData.resetUserBalances();
    }

    @Test
    void givenAValidOrderUpdateForFriend_whenAddingFriendOrderItem_itemIsAdded() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), friendId1a, productId, 1.0, "KG");
        assertEquals(1, updatedOrderItem.getItemsAdded());
        assertEquals(1.0, updatedOrderItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertNull(updatedOrderItem.getOrderDeliveredQty());
        assertEquals("KG", updatedOrderItem.getOrderUnitOfMeasure());
        assertEquals("Cassa", updatedOrderItem.getBoxUnitOfMeasure());
        assertEquals(8.5, updatedOrderItem.getBoxWeight().doubleValue(), 0.001);
        assertEquals(1.55, updatedOrderItem.getUnitPrice().doubleValue(), 0.001);
        assertEquals(1.55, updatedOrderItem.getOrderTotalAmount().doubleValue(), 0.001);

        List<UserOrderItemDTO> userItems = getUserOrderItems(computedOrder.getId(), friendId1a);
        assertEquals(6, userItems.size());
        assertTrue(userItems.stream().filter(item -> !item.getProductId().equalsIgnoreCase(productId)).allMatch(this::itemNotOrdered));

        UserOrderItemDTO orderedItem = userItems.stream()
                .filter(item -> item.getProductId().equalsIgnoreCase(productId))
                .findAny()
                .get();

        assertEquals(1.0, orderedItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals("KG", orderedItem.getOrderUnitOfMeasure());
        assertNull(orderedItem.getOrderDeliveredQty());
        assertEquals(1.55, orderedItem.getOrderTotalAmount().doubleValue(), 0.001);

        List<OpenOrderDTO> openOrder = getOpenOrder();
        assertEquals(1, openOrder.size());

        OpenOrderDTO openOrderDTO = openOrder.get(0);
        assertEquals(computedOrder.getId().toUpperCase(), openOrderDTO.getId());
        assertEquals(orderTypeComputed.getId().toUpperCase(), openOrderDTO.getOrderTypeId());
        assertEquals(orderTypeComputed.getDescription(), openOrderDTO.getOrderTypeName());
        assertNull(openOrderDTO.getExternalLink());
        assertEquals(computedOrder.getDeliveryDate(), openOrderDTO.getDeliveryDate());
        assertEquals(computedOrder.getDueDate(), openOrderDTO.getDueDate());
        assertEquals(computedOrder.getDueHour(), openOrderDTO.getDueHour());

        List<OpenOrderDTO.OpenOrderSummaryDTO> userOrders = openOrderDTO.getUserOrders();
        assertEquals(1, userOrders.size());
        assertEquals(friendId1a, userOrders.get(0).getUserId());
        assertEquals(1, userOrders.get(0).getItemsCount());
        assertEquals(1.55, userOrders.get(0).getTotalAmount().doubleValue(), 0.001);
    }

    @Test
    void givenOrdersOfUserAndFriend_whenGettingOpenOrders_thenAllItemAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        addOrdersForUserAndFriends();

        List<OpenOrderDTO> openOrder = getOpenOrder();
        assertEquals(1, openOrder.size());

        OpenOrderDTO openOrderDTO = openOrder.get(0);
        List<OpenOrderDTO.OpenOrderSummaryDTO> userOrders = openOrderDTO.getUserOrders();
        assertEquals(3, userOrders.size());

        Map<String, OpenOrderDTO.OpenOrderSummaryDTO> userOrdersMap = userOrders.stream()
                .collect(Collectors.toMap(OpenOrderDTO.OpenOrderSummaryDTO::getUserId, Function.identity()));

        OpenOrderDTO.OpenOrderSummaryDTO userOrderFriend1a = userOrdersMap.get(friendId1a);
        assertEquals(3, userOrderFriend1a.getItemsCount());
        assertEquals(9.55, userOrderFriend1a.getTotalAmount().doubleValue(), 0.001);

        OpenOrderDTO.OpenOrderSummaryDTO userOrderFriend1b = userOrdersMap.get(friendId1b);
        assertEquals(2, userOrderFriend1b.getItemsCount());
        assertEquals(6.73, userOrderFriend1b.getTotalAmount().doubleValue(), 0.001);

        OpenOrderDTO.OpenOrderSummaryDTO userOrderUser1 = userOrdersMap.get(userId1);
        assertEquals(1, userOrderUser1.getItemsCount());
        assertEquals(2.33, userOrderUser1.getTotalAmount().doubleValue(), 0.001);
    }

    @Test
    void givenAnOrderUpdateForFriendOfOtherUser_whenAddingFriendOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(friendId2, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAccountedOrderWithSummary_whenGettingFriendOrders_thenFriendOrdersAreReturned() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("user1", "password");

        List<UserOrderItemDTO> friendsOrderItems = getFriendsOrderItems(computedOrder.getId());

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        assertEquals(3, friendsOrderItems.size());
        Map<String, UserOrderItemDTO> byProduct = friendsOrderItems.stream()
                .collect(Collectors.toMap(UserOrderItemDTO::getProductId, Function.identity()));

        UserOrderItemDTO product1 = byProduct.get(productId1.toUpperCase());
        assertEquals(4.5, product1.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(4.5, product1.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product1.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product1.isAccounted());

        UserOrderItemDTO product2 = byProduct.get(productId2.toUpperCase());
        assertEquals(5.5, product2.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(5.5, product2.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product2.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product2.isAccounted());

        UserOrderItemDTO product3 = byProduct.get(productId3.toUpperCase());
        assertEquals(1.0, product3.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(1.0, product3.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product3.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product3.isAccounted());
    }

    @Test
    void givenAnAccountedOrderWithSummary_whenGettingFriendOrdersForProduct_thenFriendOrdersAreReturned() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("user1", "password");

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        Map<String, OrderItemByProductDTO> productFriendItems1 = getProductFriendItems(computedOrder.getId(), productId1);
        assertEquals(3, productFriendItems1.size());

        OrderItemByProductDTO productFriendItems1Friend1a = productFriendItems1.get(friendId1a);
        assertNull(productFriendItems1Friend1a.getDeliveredQty());
        assertEquals(1.0, productFriendItems1Friend1a.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1Friend1b = productFriendItems1.get(friendId1b);
        assertNull(productFriendItems1Friend1b.getDeliveredQty());
        assertEquals(2.0, productFriendItems1Friend1b.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1User1 = productFriendItems1.get(userId1);
        assertNull(productFriendItems1User1.getDeliveredQty());
        assertEquals(1.5, productFriendItems1User1.getRequestedQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> productFriendItems2 = getProductFriendItems(computedOrder.getId(), productId2);
        assertEquals(2, productFriendItems2.size());

        OrderItemByProductDTO productFriendItems2Friend1a = productFriendItems2.get(friendId1a);
        assertNull(productFriendItems2Friend1a.getDeliveredQty());
        assertEquals(3.0, productFriendItems2Friend1a.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems2Friend1b = productFriendItems2.get(friendId1b);
        assertNull(productFriendItems2Friend1b.getDeliveredQty());
        assertEquals(2.5, productFriendItems2Friend1b.getRequestedQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> productFriendItems3 = getProductFriendItems(computedOrder.getId(), productId3);
        assertEquals(1, productFriendItems3.size());

        OrderItemByProductDTO productFriendItems3User1 = productFriendItems3.get(friendId1a);
        assertNull(productFriendItems3User1.getDeliveredQty());
        assertEquals(1.0, productFriendItems3User1.getRequestedQty().doubleValue(), 0.001);
    }

    @Test
    void givenAnAccountedOrderWithSummaryWithChangedQuantities_whenGettingFriendOrders_thenFriendOrdersAreReturned() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("manager", "password");
        changeUserOrderItemQuantity(computedOrder.getId(), productId1, userId1, 4.23);
        changeUserOrderItemQuantity(computedOrder.getId(), productId2, userId1, 7.65);
        changeUserOrderItemQuantity(computedOrder.getId(), productId3, userId1, 2.0);

        mockMvcGoGas.loginAs("user1", "password");
        List<UserOrderItemDTO> friendsOrderItems = getFriendsOrderItems(computedOrder.getId());


        assertEquals(3, friendsOrderItems.size());
        Map<String, UserOrderItemDTO> byProduct = friendsOrderItems.stream()
                .collect(Collectors.toMap(UserOrderItemDTO::getProductId, Function.identity()));

        UserOrderItemDTO product1 = byProduct.get(productId1.toUpperCase());
        assertEquals(4.5, product1.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(4.23, product1.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product1.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product1.isAccounted());

        UserOrderItemDTO product2 = byProduct.get(productId2.toUpperCase());
        assertEquals(5.5, product2.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(7.65, product2.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product2.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product2.isAccounted());

        UserOrderItemDTO product3 = byProduct.get(productId3.toUpperCase());
        assertEquals(1.0, product3.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(2.0, product3.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(0, product3.getFriendOrderSum().doubleValue(), 0.001);
        assertFalse(product3.isAccounted());
    }

    @Test
    void givenAnAccountedOrderWithSummary_whenChangingFriendDeliveredQuantityForProduct_thenFriendQuantityIsUpdated() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("manager", "password");
        changeUserOrderItemQuantity(computedOrder.getId(), productId1, userId1, 4.23);
        changeUserOrderItemQuantity(computedOrder.getId(), productId2, userId1, 7.65);
        changeUserOrderItemQuantity(computedOrder.getId(), productId3, userId1, 2.0);

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, OrderItemByProductDTO> productFriendItems1 = getProductFriendItems(computedOrder.getId(), productId1);
        assertEquals(3, productFriendItems1.size());

        OrderItemByProductDTO productFriendItems1Friend1a = productFriendItems1.get(friendId1a);
        assertNull(productFriendItems1Friend1a.getDeliveredQty());
        assertEquals(1.0, productFriendItems1Friend1a.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1Friend1b = productFriendItems1.get(friendId1b);
        assertNull(productFriendItems1Friend1b.getDeliveredQty());
        assertEquals(2.0, productFriendItems1Friend1b.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1User1 = productFriendItems1.get(userId1);
        assertNull(productFriendItems1User1.getDeliveredQty());
        assertEquals(1.5, productFriendItems1User1.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO updateResponseFriend1a = mockMvcGoGas.putDTO("/api/order/friend/" + computedOrder.getId() + "/product/" + productId1 + "/item/" + productFriendItems1Friend1a.getOrderItemId(), BigDecimal.valueOf(1.125), OrderItemByProductDTO.class);
        assertEquals(productFriendItems1User1.getOrderItemId(), updateResponseFriend1a.getOrderItemId());
        assertEquals(3.105, updateResponseFriend1a.getDeliveredQty().doubleValue(), 0.0001);

        OrderItemByProductDTO updateResponseFriend1b = mockMvcGoGas.putDTO("/api/order/friend/" + computedOrder.getId() + "/product/" + productId1 + "/item/" + productFriendItems1Friend1b.getOrderItemId(), BigDecimal.valueOf(1.870), OrderItemByProductDTO.class);
        assertEquals(productFriendItems1User1.getOrderItemId(), updateResponseFriend1b.getOrderItemId());
        assertEquals(1.235, updateResponseFriend1b.getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> productFriendItems1Updated = getProductFriendItems(computedOrder.getId(), productId1);
        assertEquals(3, productFriendItems1Updated.size());

        OrderItemByProductDTO productFriendItems1Friend1aUpdated = productFriendItems1Updated.get(friendId1a);
        assertEquals(1.125, productFriendItems1Friend1aUpdated.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(1.0, productFriendItems1Friend1aUpdated.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1Friend1bUpdated = productFriendItems1Updated.get(friendId1b);
        assertEquals(1.870, productFriendItems1Friend1bUpdated.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(2.0, productFriendItems1Friend1bUpdated.getRequestedQty().doubleValue(), 0.001);

        OrderItemByProductDTO productFriendItems1User1Updated = productFriendItems1Updated.get(userId1);
        assertEquals(1.235, productFriendItems1User1Updated.getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(1.5, productFriendItems1User1Updated.getRequestedQty().doubleValue(), 0.001);
    }

    @Test
    void givenAnAccountedOrderWithSummary_whenAccountingFriendOrders_thenFriendBalancesAreUpdated() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("manager", "password");
        changeUserOrderItemQuantity(computedOrder.getId(), productId1, userId1, 4.23);
        changeUserOrderItemQuantity(computedOrder.getId(), productId2, userId1, 7.65);
        changeUserOrderItemQuantity(computedOrder.getId(), productId3, userId1, 2.0);

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, OrderItemByProductDTO> productFriendItems1 = getProductFriendItems(computedOrder.getId(), productId1);
        assertEquals(3, productFriendItems1.size());

        OrderItemByProductDTO productFriendItems1Friend1a = productFriendItems1.get(friendId1a);
        OrderItemByProductDTO productFriendItems1Friend1b = productFriendItems1.get(friendId1b);

        changeDeliveredQuantityForFriend(productId1, productFriendItems1Friend1a, 1.125);
        changeDeliveredQuantityForFriend(productId1, productFriendItems1Friend1b, 1.870);

        accountFriendProduct(productId1, true);

        Map<String, OrderItemByProductDTO> productFriendItems2 = getProductFriendItems(computedOrder.getId(), productId2);
        assertEquals(2, productFriendItems2.size());

        OrderItemByProductDTO productFriendItems2Friend1a = productFriendItems2.get(friendId1a);
        OrderItemByProductDTO productFriendItems2Friend1b = productFriendItems2.get(friendId1b);

        changeDeliveredQuantityForFriend(productId2, productFriendItems2Friend1a, 2.856);
        changeDeliveredQuantityForFriend(productId2, productFriendItems2Friend1b, 4.794);

        accountFriendProduct(productId2, true);

        Map<String, OrderItemByProductDTO> productFriendItems3 = getProductFriendItems(computedOrder.getId(), productId3);
        assertEquals(1, productFriendItems3.size());

        OrderItemByProductDTO productFriendItems3Friend1a = productFriendItems3.get(friendId1a);

        changeDeliveredQuantityForFriend(productId3, productFriendItems3Friend1a, 2.0);

        accountFriendProduct(productId3, true);

        UserBalanceSummaryDTO balanceUser1 = getBalance(userId1);
        assertEquals(0.0, balanceUser1.getBalance().doubleValue(), 0.001);
        assertTrue(balanceUser1.getEntries().isEmpty());

        checkFriendBalance("friendId1a", friendId1a, -13.18);
        checkFriendBalance("friendId1b", friendId1b, -9.85);
    }

    @Test
    void givenAnAccountedOrderWithSummary_whenUndoAccountingFriendOrders_thenFriendBalancesAreCorrectlyUpdated() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        String productId1 = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        mockMvcGoGas.loginAs("user1", "password");
        addOrdersForUserAndFriends();

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("manager", "password");
        changeUserOrderItemQuantity(computedOrder.getId(), productId1, userId1, 4.23);
        changeUserOrderItemQuantity(computedOrder.getId(), productId2, userId1, 7.65);
        changeUserOrderItemQuantity(computedOrder.getId(), productId3, userId1, 2.0);

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, OrderItemByProductDTO> productFriendItems1 = getProductFriendItems(computedOrder.getId(), productId1);
        assertEquals(3, productFriendItems1.size());

        OrderItemByProductDTO productFriendItems1Friend1a = productFriendItems1.get(friendId1a);
        OrderItemByProductDTO productFriendItems1Friend1b = productFriendItems1.get(friendId1b);

        changeDeliveredQuantityForFriend(productId1, productFriendItems1Friend1a, 1.125);
        changeDeliveredQuantityForFriend(productId1, productFriendItems1Friend1b, 1.870);

        accountFriendProduct(productId1, true);

        Map<String, OrderItemByProductDTO> productFriendItems2 = getProductFriendItems(computedOrder.getId(), productId2);
        assertEquals(2, productFriendItems2.size());

        OrderItemByProductDTO productFriendItems2Friend1a = productFriendItems2.get(friendId1a);
        OrderItemByProductDTO productFriendItems2Friend1b = productFriendItems2.get(friendId1b);

        changeDeliveredQuantityForFriend(productId2, productFriendItems2Friend1a, 2.856);
        changeDeliveredQuantityForFriend(productId2, productFriendItems2Friend1b, 4.794);

        accountFriendProduct(productId2, true);

        Map<String, OrderItemByProductDTO> productFriendItems3 = getProductFriendItems(computedOrder.getId(), productId3);
        assertEquals(1, productFriendItems3.size());

        OrderItemByProductDTO productFriendItems3Friend1a = productFriendItems3.get(friendId1a);

        changeDeliveredQuantityForFriend(productId3, productFriendItems3Friend1a, 2.0);

        accountFriendProduct(productId3, true);

        UserBalanceSummaryDTO balanceUser1 = getBalance(userId1);
        assertEquals(0.0, balanceUser1.getBalance().doubleValue(), 0.001);
        assertTrue(balanceUser1.getEntries().isEmpty());

        checkFriendBalance("friendId1a", friendId1a, -13.18);
        checkFriendBalance("friendId1b", friendId1b, -9.85);

        accountFriendProduct(productId1, false);

        checkFriendBalance("friendId1a", friendId1a, -11.44);
        checkFriendBalance("friendId1b", friendId1b, -6.95);

        accountFriendProduct(productId2, false);

        checkFriendBalance("friendId1a", friendId1a, -7.3);
        checkEmptyFriendBalance("friendId1b", friendId1b);
    }

    private void checkFriendBalance(String friendUsername, String friendId, double expectedBalance) throws Exception {
        mockMvcGoGas.loginAs(friendUsername, "password");
        UserBalanceSummaryDTO balanceFriend1aAfterUndo = getBalance(friendId);
        assertEquals(expectedBalance, balanceFriend1aAfterUndo.getBalance().doubleValue(), 0.001);
        assertEquals(1, balanceFriend1aAfterUndo.getEntries().size());
        assertEquals(expectedBalance, balanceFriend1aAfterUndo.getEntries().get(0).getAmount().doubleValue(), 0.0001);
        assertEquals(computedOrder.getId().toUpperCase(), balanceFriend1aAfterUndo.getEntries().get(0).getOrderId());
    }

    private void checkEmptyFriendBalance(String friendUsername, String friendId) throws Exception {
        mockMvcGoGas.loginAs(friendUsername, "password");
        UserBalanceSummaryDTO balanceFriend1aAfterUndo = getBalance(friendId);
        assertEquals(0.0, balanceFriend1aAfterUndo.getBalance().doubleValue(), 0.001);
        assertTrue(balanceFriend1aAfterUndo.getEntries().isEmpty());
    }

    private void changeDeliveredQuantityForFriend(String productId1, OrderItemByProductDTO productFriendItems1Friend1a, double v) throws Exception {
        mockMvcGoGas.putDTO("/api/order/friend/" + computedOrder.getId() + "/product/" + productId1 + "/item/" + productFriendItems1Friend1a.getOrderItemId(), BigDecimal.valueOf(v), OrderItemByProductDTO.class);
    }

    private void accountFriendProduct(String productId1, boolean charge) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        BasicResponseDTO response = mockMvcGoGas.putDTO("/api/order/friend/" + computedOrder.getId() + "/product/" + productId1 + "/accounted", charge, BasicResponseDTO.class);
        assertEquals("OK", response.getData());
    }

    //TODO:
    // test invalid requests,
    // test unauthorized,
    // test friend delivered quantity greater than actual sum of delivered quantity
    // test accounting not possible if quantities not matching
    // test change delivered quantity already accounted (NOT ALLOWED)
    // test partial accounting
    // test available friends
    // test add friend order
    // test export
    // check ROUNDING FOR BALANCE!!!!

    private void addOrdersForUserAndFriends() throws Exception {
        String productId = productsByCodeComputed.get("MELE1").getId();
        String productId2 = productsByCodeComputed.get("PATATE").getId();
        String productId3 = productsByCodeComputed.get("BIRRA1").getId();

        sendUserOrderItem(computedOrder.getId(), friendId1a, productId, 1.0, "KG");
        sendUserOrderItem(computedOrder.getId(), friendId1b, productId, 2.0, "KG");
        sendUserOrderItem(computedOrder.getId(), userId1, productId, 1.5, "KG");

        sendUserOrderItem(computedOrder.getId(), friendId1a, productId2, 3.0, "KG");
        sendUserOrderItem(computedOrder.getId(), friendId1b, productId2, 2.5, "KG");

        sendUserOrderItem(computedOrder.getId(), friendId1a, productId3, 1.0, "PZ");
    }

    private List<OpenOrderDTO> getOpenOrder() throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/user/open", OpenOrderDTO.class);
    }

    private List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/user/" + orderId + "/items", UserOrderItemDTO.class, Map.of("userId", List.of(userId)));
    }

    private List<UserOrderItemDTO> getFriendsOrderItems(String orderId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/friend/" + orderId + "/items", UserOrderItemDTO.class);
    }

    private SmallUserOrderItemDTO sendUserOrderItem(String orderId, String userId, String productId, Double qty, String um) throws Exception {
        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId, productId, qty, um);
        return mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    private OrderItemUpdateRequest buildOrderItemUpdateRequest(String userId, String productId, Double qty, String um) {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productId);
        request.setQuantity(Optional.ofNullable(qty).map(BigDecimal::valueOf).orElse(null));
        request.setUnitOfMeasure(um);
        return request;
    }

    private boolean itemNotOrdered(UserOrderItemDTO item) {
        return item.getOrderRequestedQty() == null && item.getOrderDeliveredQty() == null;
    }

    void closeOrder(String orderId) throws Exception {
        mockOrdersData.forceOrderDates(computedOrder.getId(), LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/close", null, BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    void changeUserOrderItemQuantity(String orderId, String productCode, String userId, double quantity) throws Exception {
        Map<String, OrderItemByProductDTO> items = getProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + items.get(userId).getOrderItemId(), BigDecimal.valueOf(quantity), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    Map<String, OrderItemByProductDTO> getProductItems(String orderId, String productId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product/" + productId, OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    Map<String, OrderItemByProductDTO> getProductFriendItems(String orderId, String productId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/friend/" + orderId + "/product/" + productId, OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    private UserBalanceSummaryDTO getBalance(String userId) throws Exception {
        return mockMvcGoGas.getDTO("/api/accounting/user/balance/" + userId, UserBalanceSummaryDTO.class);
    }
}
