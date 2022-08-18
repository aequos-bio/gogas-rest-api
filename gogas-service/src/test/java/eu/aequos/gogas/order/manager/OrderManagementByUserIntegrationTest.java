package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderByUserDTO;
import eu.aequos.gogas.dto.OrderItemByUserDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderManagementByUserIntegrationTest extends OrderManagementBaseIntegrationTest {

    private Order orderComputed;
    private Order orderNotComputed;
    private Order orderExternal;

    @BeforeEach
    void setUp() {
        orderComputed = mockOrdersData.createOrder(orderTypeComputed, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRA1", 10),
                entry("BIRRA2", 8),
                entry("MELE1", 1),
                entry("MELE2", 3),
                entry("PATATE", 1)
        );

        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(orderComputed, productsByCodeComputed.get(productCode), quantity));

        Map<String, Map<String, Double>> userQuantities = Map.of(
                userId1,
                Map.ofEntries(
                        entry("BIRRA1", 1.0),
                        entry("MELE1", 2.5),
                        entry("MELE2", 1.5),
                        entry("PATATE", 10.8)
                ),
                userId2,
                Map.ofEntries(
                        entry("BIRRA2", 2.0),
                        entry("MELE1", 2.0),
                        entry("PATATE", 4.7)
                ),
                userId3,
                Map.ofEntries(
                        entry("BIRRA2", 2.0),
                        entry("MELE1", 4.0),
                        entry("MELE2", 1.5)
                )
        );

        createUserOrders(orderComputed, productsByCodeComputed, userQuantities);

        orderNotComputed = mockOrdersData.createOrder(orderTypeNotComputed, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        Map<String, Integer> boxQuantitiesNotComputed = Map.ofEntries(
                entry("COSTINE", 1),
                entry("FILETTO", 1),
                entry("FEGATO", 1),
                entry("FETTINE", 1),
                entry("COPPA", 1)
        );

        boxQuantitiesNotComputed
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(orderNotComputed, productsByCodeNotComputed.get(productCode), quantity));

        Map<String, Map<String, Double>> userQuantitiesNotComputed = Map.of(
                userId1,
                Map.ofEntries(
                        entry("COSTINE", 0.7),
                        entry("FEGATO", 0.8),
                        entry("FETTINE", 0.5)
                ),
                userId2,
                Map.ofEntries(
                        entry("FILETTO", 0.5),
                        entry("FEGATO", 0.4),
                        entry("COPPA", 0.35)
                ),
                userId3,
                Map.ofEntries(
                        entry("COSTINE", 1.0),
                        entry("FILETTO", 0.5),
                        entry("FEGATO", 0.6),
                        entry("FETTINE", 0.4),
                        entry("COPPA", 0.25)
                )
        );

        createUserOrders(orderNotComputed, productsByCodeNotComputed, userQuantitiesNotComputed);

        orderExternal = mockOrdersData.createOrder(orderTypeExternal, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteOrder(orderComputed.getId());
    }

    @Test
    void givenAnOrderWithComputedAmount_whenGettingSummaryByUser_thenAllValuesAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 4, 25.735);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 17.215);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 3, 16.05);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingSummaryByUser_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderComputed.getId() + "/byUser/list")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingSummaryByUser_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderComputed.getId() + "/byUser/list")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenGettingSummaryByUser_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/order/manage/" + UUID.randomUUID() + "/byUser/list")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnOrderWithComputedAmount_whenGettingItemsByUser_thenAllValuesAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByUserDTO> userItems1 = mockMvcGoGas.getDTOList("/api/order/manage/" + orderComputed.getId() + "/byUser/" + userId1, OrderItemByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByUserDTO::getProductName, Function.identity()));

        verifyUserOrderItem(userItems1.get("BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone"), "PZ", 1.0, 3.65);
        verifyUserOrderItem(userItems1.get("MELE CRIMSON CRISP - Roncaglia"), "KG", 2.5, 3.875);
        verifyUserOrderItem(userItems1.get("MELE OPAL - Roncaglia"), "KG", 1.5, 2.55);
        verifyUserOrderItem(userItems1.get("PATATE GIALLE DI MONTAGNA - Abbiate Valerio"), "KG", 10.8, 15.66);

        Map<String, OrderItemByUserDTO> userItems2 = mockMvcGoGas.getDTOList("/api/order/manage/" + orderComputed.getId() + "/byUser/" + userId2, OrderItemByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByUserDTO::getProductName, Function.identity()));

        verifyUserOrderItem(userItems2.get("BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone"), "PZ", 2.0, 7.3);
        verifyUserOrderItem(userItems2.get("MELE CRIMSON CRISP - Roncaglia"), "KG", 2.0, 3.1);
        verifyUserOrderItem(userItems2.get("PATATE GIALLE DI MONTAGNA - Abbiate Valerio"), "KG", 4.7, 6.815);

        Map<String, OrderItemByUserDTO> userItems3 = mockMvcGoGas.getDTOList("/api/order/manage/" + orderComputed.getId() + "/byUser/" + userId3, OrderItemByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByUserDTO::getProductName, Function.identity()));

        verifyUserOrderItem(userItems3.get("BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone"), "PZ", 2.0, 7.3);
        verifyUserOrderItem(userItems3.get("MELE CRIMSON CRISP - Roncaglia"), "KG", 4.0, 6.2);
        verifyUserOrderItem(userItems3.get("MELE OPAL - Roncaglia"), "KG", 1.5, 2.55);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingItemsByUser_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderComputed.getId() + "/byUser/" + userId1)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingItemsByUser_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderComputed.getId() + "/byUser/" + userId1)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenGettingItemsByUser_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/order/manage/" + UUID.randomUUID() + "/byUser/" + userId1)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingUser_whenGettingItemsByUser_thenNoItemsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        List<OrderItemByUserDTO> dtoList = mockMvcGoGas.getDTOList("/api/order/manage/" + orderComputed.getId() + "/byUser/" + UUID.randomUUID(), OrderItemByUserDTO.class);
        assertTrue(dtoList.isEmpty());
    }

    @Test
    void givenAnOrderWithComputedAmount_whenAddingShippingCost_thenCostIsDistributedOnUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderComputed.getId() + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 4, 25.735, 4.36);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 17.215, 2.92);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 3, 16.05, 2.72);
    }

    @Test
    void givenAnOrderWithNotComputedAmount_whenGettingSummaryByUser_thenAllAmountsAreZero() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 0.0);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 0.0);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 0.0);
    }

    @Test
    void givenAnOrderWithNotComputedAmount_whenAddingUserCost_thenUserAmountsAreSaved() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(49.5), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 23.87);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 0.0);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 49.5);
    }

    @Test
    void givenAnOrderWithNotComputedAmount_whenAddingAllUserCosts_thenAllUserAmountsAreSaved() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(35.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(49.5), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 23.87);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 35.0);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 49.5);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAddingUserCosts_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenAddingUserCosts_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenAddingUserCosts_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/byUser/" + userId1, BigDecimal.valueOf(23.87))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingUser_whenAddingUserCosts_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + UUID.randomUUID(), BigDecimal.valueOf(23.87))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANegativeCost_whenAddingUserCosts_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(-1.0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("updateAmountByUser.cost: must be greater than or equal to 0")));
    }

    @Test
    void givenAnOrderWithNotComputedAmountWithAllUsersCost_whenGettingAvailableUsers_thenNoUserIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(35.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(49.5), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        Set<String> userIds = Set.of(userId1, userId2, userId3);

        List<SelectItemDTO> availableUsers = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/availableUsers", SelectItemDTO.class);
        assertTrue(availableUsers.stream().noneMatch(item -> userIds.contains(item.getId())));
    }

    @Test
    void givenAnOrderWithNotComputedAmountWithSomeUsersCost_whenGettingAvailableUsers_thenOtherUsersAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(49.5), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        Set<String> userIds = Set.of(userId1, userId3);

        List<SelectItemDTO> availableUsers = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/availableUsers", SelectItemDTO.class);
        assertTrue(availableUsers.stream().noneMatch(item -> userIds.contains(item.getId())));
        assertTrue(availableUsers.stream().anyMatch(item -> userId2.equals(item.getId())));
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingAvailableUsers_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderNotComputed.getId() + "/byUser/availableUsers")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingAvailableUsers_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderNotComputed.getId() + "/byUser/availableUsers")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenGettingAvailableUsers_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + UUID.randomUUID() + "/byUser/availableUsers")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnOrderWithNotComputedAmount_whenRemovingAUserCosts_thenUserAmountIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(23.87), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(35.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(49.5), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        BasicResponseDTO deleteResponse = mockMvcGoGas.deleteDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BasicResponseDTO.class);
        assertEquals("OK", deleteResponse.getData());

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 23.87);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 0.0);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 49.5);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenRemovingAUserCosts_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenRemovingAUserCosts_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenRemovingAUserCosts_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.delete("/api/order/manage/" + UUID.randomUUID() + "/byUser/" + userId1)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingUser_whenRemovingAUserCosts_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.delete("/api/order/manage/" + UUID.randomUUID() + "/byUser/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnOrderWithNotComputedAmount_whenAddingShippingCost_thenCostIsDistributedOnUsers() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(25.0), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(15.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(10.0), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(5.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 25.0, 2.5);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 15.0, 1.5);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 10.0, 1.0);
    }

    @Test
    void givenAnOrderWithNotComputedAmountWithPartialCost_whenAddingShippingCost_thenShippingCostIsDistributedOnExistingCost() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(25.0), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(15.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(5.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 25.0, 3.125);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 15.0, 1.875);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 0.0, 0.0);
    }

    @Test
    void givenAnOrderWithNotComputedAmountWithShippingCosts_whenChangingUserAmount_thenShippingCostIsRedistributed() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(25.0), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId2, BigDecimal.valueOf(15.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId3, BigDecimal.valueOf(10.0), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        mockMvcGoGas.postDTOList("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(5.0), OrderByUserDTO.class);

        BasicResponseDTO updateResponse4 = mockMvcGoGas.postDTO("/api/order/manage/" + orderNotComputed.getId() + "/byUser/" + userId1, BigDecimal.valueOf(12.0), BasicResponseDTO.class);
        assertNotNull(updateResponse4);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.getDTOList("/api/order/manage/" + orderNotComputed.getId() + "/byUser/list", OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 3, 12.0, 1.62);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 3, 15.0, 2.03);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 5, 10.0, 1.35);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAddingShippingCost_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(5.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenAddingShippingCost_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(5.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrder_whenAddingShippingCost_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/shippingCost", BigDecimal.valueOf(5.0))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANegativeCost_whenAddingShippingCosts_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderNotComputed.getId() + "/shippingCost", BigDecimal.valueOf(-1.0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("updateShippingCost.cost: must be greater than or equal to 0")));
    }

    @Test
    void givenAnExternalOrder_whenAddingShippingCost_thenCostIsDistributedOnUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderExternal.getId() + "/byUser/" + userId1, BigDecimal.valueOf(25.0), BasicResponseDTO.class);
        assertNotNull(updateResponse);

        BasicResponseDTO updateResponse2 = mockMvcGoGas.postDTO("/api/order/manage/" + orderExternal.getId() + "/byUser/" + userId2, BigDecimal.valueOf(15.0), BasicResponseDTO.class);
        assertNotNull(updateResponse2);

        BasicResponseDTO updateResponse3 = mockMvcGoGas.postDTO("/api/order/manage/" + orderExternal.getId() + "/byUser/" + userId3, BigDecimal.valueOf(10.0), BasicResponseDTO.class);
        assertNotNull(updateResponse3);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderExternal.getId() + "/shippingCost", BigDecimal.valueOf(5.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        verifyUserOrder(userTotals.get(userId1), "user1 user1", 0, 25.0, 2.5);
        verifyUserOrder(userTotals.get(userId2), "user2 user2", 0, 15.0, 1.5);
        verifyUserOrder(userTotals.get(userId3), "user3 user3", 0, 10.0, 1.0);
    }

    private void verifyUserOrder(OrderByUserDTO order, String userName, int itemsCount, double amount) {
        assertEquals(userName, order.getUserFullName());
        assertEquals(itemsCount, order.getOrderedItemsCount());
        assertEquals(amount, order.getNetAmount().doubleValue(), 0.001);
        assertEquals(amount, order.getTotalAmount().doubleValue(), 0.001);
        assertFalse(order.isNegativeBalance());
        assertNull(order.getShippingCost());
    }

    private void verifyUserOrder(OrderByUserDTO order, String userName, int itemsCount, double amount, double shippingCost) {
        assertEquals(userName, order.getUserFullName());
        assertEquals(itemsCount, order.getOrderedItemsCount());
        assertEquals(amount, order.getNetAmount().doubleValue(), 0.001);
        assertEquals(shippingCost, order.getShippingCost().doubleValue(), 0.001);
        assertEquals(amount + shippingCost, order.getTotalAmount().doubleValue(), 0.001);
        assertFalse(order.isNegativeBalance());
    }

    private void verifyUserOrderItem(OrderItemByUserDTO orderItemm, String um, double deliveredQty, double amount) {
        assertNotNull(orderItemm.getOrderItemId());
        assertEquals(um, orderItemm.getUnitOfMeasure());
        assertEquals(amount, orderItemm.getTotalAmount().doubleValue(), 0.001);
        assertEquals(deliveredQty, orderItemm.getDeliveredQty().doubleValue(), 0.001);
    }

    private void createUserOrders(Order order, Map<String, Product> productsByCode, Map<String, Map<String, Double>> userQuantities) {
        userQuantities
                .forEach((userId, quantities) -> quantities
                        .forEach((productCode, quantity) ->
                                mockOrdersData.createDeliveredUserOrderItem(order.getId(), userId, productsByCode.get(productCode), quantity)
                        )
                );
    }
}
