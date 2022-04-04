package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.OrderByProductDTO;
import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.dto.OrderItemByProductDTO;
import eu.aequos.gogas.service.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderCloseIntegrationTest extends OrderManagementBaseIntegrationTest {

    @MockBean
    private ConfigurationService configurationService;

    @Test
    void givenAnOpenOrderNotExpired_whenClosingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);
        invalidAction(orderId, "close", "Order is not expired");
    }

    @Test
    void givenAnExpiredEmptyOpenOrder_whenClosingOrder_thenOrderIsClosed() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 1, "Chiuso", "gestisci,riapri,contabilizza", true, false);
    }

    @Test
    void givenANotEmptyOpenOrder_whenClosingOrder_thenUserQuantitiesAreCorrectlyCopied() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderItem(items1.get(userId1), 1.0, 1.0, "PZ", false);
        verifyOrderItem(items1.get(userId2), 2.0, 2.0, "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderItem(items2.get(userId1), 3.0, 3.0, "KG", false);
        verifyOrderItem(items2.get(userId2), 8.5, 8.5, "KG", false);
        verifyOrderItem(items2.get(userId3), 3.5, 3.5, "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "PATATE");
        verifyOrderItem(items3.get(userId1), 4.0, 4.0, "KG", false);
        verifyOrderItem(items3.get(userId2), 4.5, 4.5, "KG", false);
        verifyOrderItem(items3.get(userId3), 5.5, 5.5, "KG", false);
    }

    @Test
    void givenARoundingTypeByThresholdHal_whenClosingOrder_thenSupplierOrderBoxesAreCreatedAccordingly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 3.0, 3.0, 3.0, 3.0, 2);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 2.0, 17.0, 3);
        verifyProductDTO(products, "PATATE", 1.273, 14.0, 1.0, 11.0, 3);
    }

    @Test
    void givenARoundingTypeByThresholdLow_whenClosingOrder_thenSupplierOrderBoxesAreCreatedAccordingly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.1));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 3.0, 3.0, 3.0, 3.0, 2);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 2.0, 17.0, 3);
        verifyProductDTO(products, "PATATE", 1.273, 14.0, 2.0, 22.0, 3);
    }

    @Test
    void givenARoundingTypeCeil_whenClosingOrder_thenSupplierOrderBoxesAreCreatedAccordingly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 1);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 3.0, 3.0, 3.0, 3.0, 2);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 2.0, 17.0, 3);
        verifyProductDTO(products, "PATATE", 1.273, 14.0, 2.0, 22.0, 3);
    }

    @Test
    void givenARoundingTypeFloor_whenClosingOrder_thenSupplierOrderBoxesAreCreatedAccordingly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 2);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 3.0, 3.0, 3.0, 3.0, 2);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 1.0, 8.5, 3);
        verifyProductDTO(products, "PATATE", 1.273, 14.0, 1.0, 11.0, 3);
    }

    @Test
    void givenFriendOrdersAndNoSummaryRequired_whenClosingOrder_thenFriendsOrdersAreKeptSeparate() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersWithFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 6.0, 6.0, 6.0, 6.0, 4);
        verifyProductDTO(products, "MELE1", 2.0, 17.0, 2.0, 17.0, 4);
        verifyProductDTO(products, "MELE2", 1.471, 12.5, 1.0, 8.5, 2);
        verifyProductDTO(products, "PATATE", 1.5, 16.5, 2.0, 22.0, 4);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderItem(items1.get(userId1), 1.0, 1.0, "PZ", false);
        verifyOrderItem(items1.get(userId2), 2.0, 2.0, "PZ", false);
        verifyOrderItem(items1.get(friendId1a), 2.0, 2.0, "PZ", false);
        verifyOrderItem(items1.get(friendId2), 1.0, 1.0, "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderItem(items2.get(userId1), 3.0, 3.0, "KG", false);
        verifyOrderItem(items2.get(userId2), 8.5, 8.5, "KG", false);
        verifyOrderItem(items2.get(userId3), 3.5, 3.5, "KG", false);
        verifyOrderItem(items2.get(friendId1b), 2.0, 2.0, "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "MELE2");
        verifyOrderItem(items3.get(friendId1a), 4.0, 4.0, "KG", false);
        verifyOrderItem(items3.get(friendId2), 8.5, 8.5, "KG", false);

        Map<String, OrderItemByProductDTO> items4 = getProductItems(orderId, "PATATE");
        verifyOrderItem(items4.get(userId1), 4.0, 4.0, "KG", false);
        verifyOrderItem(items4.get(userId2), 4.5, 4.5, "KG", false);
        verifyOrderItem(items4.get(userId3), 5.5, 5.5, "KG", false);
        verifyOrderItem(items4.get(friendId1b), 2.5, 2.5, "KG", false);
    }

    @Test
    void givenFriendOrdersAndSummaryRequired_whenClosingOrder_thenFriendsOrdersAreMergedInReferenceUserSeparate() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        //force summary required
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersWithFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 6.0, 6.0, 6.0, 6.0, 2);
        verifyProductDTO(products, "MELE1", 2.0, 17.0, 2.0, 17.0, 3);
        verifyProductDTO(products, "MELE2", 1.471, 12.5, 1.0, 8.5, 2);
        verifyProductDTO(products, "PATATE", 1.5, 16.5, 2.0, 22.0, 3);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderItem(items1.get(userId1), 3.0, 3.0, "PZ", false);
        verifyOrderItem(items1.get(userId2), 3.0, 3.0, "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderItem(items2.get(userId1), 5.0, 5.0, "KG", false);
        verifyOrderItem(items2.get(userId2), 8.5, 8.5, "KG", false);
        verifyOrderItem(items2.get(userId3), 3.5, 3.5, "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "MELE2");
        verifyOrderItem(items3.get(userId1), 4.0, 4.0, "KG", false);
        verifyOrderItem(items3.get(userId2), 8.5, 8.5, "KG", false);

        Map<String, OrderItemByProductDTO> items4 = getProductItems(orderId, "PATATE");
        verifyOrderItem(items4.get(userId1), 6.5, 6.5, "KG", false);
        verifyOrderItem(items4.get(userId2), 4.5, 4.5, "KG", false);
        verifyOrderItem(items4.get(userId3), 5.5, 5.5, "KG", false);

        //restore original summary required
        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);
    }

    @Test
    void givenANotExistingOrder_whenClosingOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/action/close", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenClosingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/close", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenClosingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/close", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledOrder_whenClosingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "cancel");
        invalidAction(orderId, "close");
    }

    @Test
    void givenAnAccountedOrder_whenClosingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        invalidAction(orderId, "close");
    }

    @Test
    void givenAClosedOrder_whenClosingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        invalidAction(orderId, "close");
    }

    @Test
    void givenAWrongRoundingType_whenClosingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/close", null, new LinkedMultiValueMap<>(Map.of("roundType", List.of(String.valueOf(4)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid rounding mode: 4")));;
    }

    @Test
    void givenAClosedOrderWithModifiedQuantities_whenReopeningOrder_thenUserQuantitiesAreCorrectlyRestore() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersNoFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 1.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 2.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.0);

        performAction(orderId, "reopen");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel,chiudi", false, false);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 3.0, 3.0, 0.0, 0.0, 2);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 0.0, 0.0, 3);
        verifyProductDTO(products, "MELE1", 1.765, 15.0, 0.0, 0.0, 3);
        verifyProductDTO(products, "PATATE", 1.273, 14.0, 0.0, 0.0, 3);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderOpenedItem(items1.get(userId1), 1.0, "PZ", false);
        verifyOrderOpenedItem(items1.get(userId2), 2.0, "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderOpenedItem(items2.get(userId1), 3.0, "KG", false);
        verifyOrderOpenedItem(items2.get(userId2), 1.0, "Cassa", false);
        verifyOrderOpenedItem(items2.get(userId3), 3.5, "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "PATATE");
        verifyOrderOpenedItem(items3.get(userId1), 4.0, "KG", false);
        verifyOrderOpenedItem(items3.get(userId2), 4.5, "KG", false);
        verifyOrderOpenedItem(items3.get(userId3), 5.5, "KG", false);
    }

    @Test
    void givenAClosedOrderWithNoSummaryRequired_whenReopeningOrder_thenUserQuantitiesAreCorrectlyRestore() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersWithFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 1.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 2.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.0);
        changeUserOrderItemQuantity(orderId, "MELE2", friendId1a, 3.0);

        performAction(orderId, "reopen");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel,chiudi", false, false);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 6.0, 6.0, 0.0, 0.0, 4);
        verifyProductDTO(products, "MELE1", 2.0, 17.0, 0.0, 0.0, 4);
        verifyProductDTO(products, "MELE2", 1.471, 12.5, 0.0, 0.0, 2);
        verifyProductDTO(products, "PATATE", 1.5, 16.5, 0.0, 0.0, 4);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderOpenedItem(items1.get(userId1), 1.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(userId2), 2.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(friendId1a), 2.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(friendId2), 1.0,  "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderOpenedItem(items2.get(userId1), 3.0,  "KG", false);
        verifyOrderOpenedItem(items2.get(userId2), 1.0, "Cassa", false);
        verifyOrderOpenedItem(items2.get(userId3), 3.5,  "KG", false);
        verifyOrderOpenedItem(items2.get(friendId1b), 2.0,  "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "MELE2");
        verifyOrderOpenedItem(items3.get(friendId1a), 4.0, "KG", false);
        verifyOrderOpenedItem(items3.get(friendId2), 1.0, "Cassa", false);

        Map<String, OrderItemByProductDTO> items4 = getProductItems(orderId, "PATATE");
        verifyOrderOpenedItem(items4.get(userId1), 4.0, "KG", false);
        verifyOrderOpenedItem(items4.get(userId2), 4.5, "KG", false);
        verifyOrderOpenedItem(items4.get(userId3), 5.5, "KG", false);
        verifyOrderOpenedItem(items4.get(friendId1b), 2.5, "KG", false);
    }

    @Test
    void givenAClosedOrderWithSummaryRequired_whenReopeningOrder_thenUserQuantitiesAreCorrectlyRestore() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersWithFriends(orderId);

        when(configurationService.getBoxRoundingThreshold()).thenReturn(BigDecimal.valueOf(0.5));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("manager", "password");

        performClose(orderId, 0);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 1.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 2.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.0);
        changeUserOrderItemQuantity(orderId, "MELE2", userId1, 5.0);

        performAction(orderId, "reopen");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel,chiudi", false, false);

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", 6.0, 6.0, 0.0, 0.0, 4);
        verifyProductDTO(products, "MELE1", 2.0, 17.0, 0.0, 0.0, 4);
        verifyProductDTO(products, "MELE2", 1.471, 12.5, 0.0, 0.0, 2);
        verifyProductDTO(products, "PATATE", 1.5, 16.5, 0.0, 0.0, 4);

        Map<String, OrderItemByProductDTO> items1 = getProductItems(orderId, "BIRRA1");
        verifyOrderOpenedItem(items1.get(userId1), 1.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(userId2), 2.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(friendId1a), 2.0,  "PZ", false);
        verifyOrderOpenedItem(items1.get(friendId2), 1.0,  "PZ", false);

        Map<String, OrderItemByProductDTO> items2 = getProductItems(orderId, "MELE1");
        verifyOrderOpenedItem(items2.get(userId1), 3.0,  "KG", false);
        verifyOrderOpenedItem(items2.get(userId2), 1.0, "Cassa", false);
        verifyOrderOpenedItem(items2.get(userId3), 3.5,  "KG", false);
        verifyOrderOpenedItem(items2.get(friendId1b), 2.0,  "KG", false);

        Map<String, OrderItemByProductDTO> items3 = getProductItems(orderId, "MELE2");
        verifyOrderOpenedItem(items3.get(friendId1a), 4.0, "KG", false);
        verifyOrderOpenedItem(items3.get(friendId2), 1.0, "Cassa", false);

        Map<String, OrderItemByProductDTO> items4 = getProductItems(orderId, "PATATE");
        verifyOrderOpenedItem(items4.get(userId1), 4.0, "KG", false);
        verifyOrderOpenedItem(items4.get(userId2), 4.5, "KG", false);
        verifyOrderOpenedItem(items4.get(userId3), 5.5, "KG", false);
        verifyOrderOpenedItem(items4.get(friendId1b), 2.5, "KG", false);

        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);
    }

    @Test
    void givenANotExistingOrder_whenReopeningOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/action/reopen", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenReopeningOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/reopen", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenReopeningOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/reopen", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledOrder_whenReopeningOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "cancel");
        invalidAction(orderId, "reopen");
    }

    @Test
    void givenAnAccountedOrder_whenReopeningOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        invalidAction(orderId, "reopen");
    }

    @Test
    void givenAnOpenOrder_whenReopeningOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        invalidAction(orderId, "reopen");
    }

    private void addUserOrdersNoFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.0, "KG");
        addComputedUserOrder(orderId, userId1, "PATATE", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRA1", 1.0, "PZ");

        mockMvcGoGas.loginAs("user2", "password");
        addComputedUserOrder(orderId, userId2, "MELE1", 1.0, "Cassa");
        addComputedUserOrder(orderId, userId2, "PATATE", 4.5, "KG");
        addComputedUserOrder(orderId, userId2, "BIRRA1", 2.0, "PZ");

        mockMvcGoGas.loginAs("user3", "password");
        addComputedUserOrder(orderId, userId3, "MELE1", 3.5, "KG");
        addComputedUserOrder(orderId, userId3, "PATATE", 5.5, "KG");
    }

    private void addUserOrdersWithFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.0, "KG");
        addComputedUserOrder(orderId, userId1, "PATATE", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRA1", 1.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "BIRRA1", 2.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "MELE2", 4.0, "KG");
        addComputedUserOrder(orderId, friendId1b, "MELE1", 2.0, "KG");
        addComputedUserOrder(orderId, friendId1b, "PATATE", 2.5, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        addComputedUserOrder(orderId, userId2, "MELE1", 1.0, "Cassa");
        addComputedUserOrder(orderId, userId2, "PATATE", 4.5, "KG");
        addComputedUserOrder(orderId, userId2, "BIRRA1", 2.0, "PZ");
        addComputedUserOrder(orderId, friendId2, "MELE2", 1.0, "Cassa");
        addComputedUserOrder(orderId, friendId2, "BIRRA1", 1.0, "PZ");

        mockMvcGoGas.loginAs("user3", "password");
        addComputedUserOrder(orderId, userId3, "MELE1", 3.5, "KG");
        addComputedUserOrder(orderId, userId3, "PATATE", 5.5, "KG");
    }

    private void verifyProductDTO(Map<String, OrderByProductDTO> products, String productCode,
                                  double deliveredBoxes, double deliveredQty, double orderedBoxes, double orderedQty, int usersCount) {

        OrderByProductDTO productDTO = products.get(productsByCodeComputed.get(productCode).getId().toUpperCase());
        assertEquals(deliveredBoxes, productDTO.getDeliveredBoxes().doubleValue(), 0.001);
        assertEquals(deliveredQty, productDTO.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(orderedBoxes, productDTO.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(orderedQty, productDTO.getOrderedQty().doubleValue(), 0.001);
        assertEquals(usersCount, productDTO.getOrderingUsersCount());
    }
}
