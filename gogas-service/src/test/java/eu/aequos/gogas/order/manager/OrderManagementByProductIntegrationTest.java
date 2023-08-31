package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.persistence.entity.Order;
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

class OrderManagementByProductIntegrationTest extends OrderManagementBaseIntegrationTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = mockOrdersData.createOrder(orderTypeComputed, "2022-03-20", "2022-03-30", "2022-04-01", Order.OrderStatus.Closed);

        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRA1", 10),
                entry("BIRRA2", 8),
                entry("MELE1", 1),
                entry("MELE2", 3),
                entry("PATATE", 1)
        );

        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(order, productsByCodeComputed.get(productCode), quantity));

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

        createUserOrders(userQuantities);
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteOrder(order.getId());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingOrderDetails_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingOrderDetails_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAClosedOrder_whenGettingOrderDetails_thenAllValuesAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + order.getId(), OrderDetailsDTO.class);
        assertNotNull(orderDetails);
        assertEquals(orderTypeComputed.getId().toUpperCase(), orderDetails.getOrderTypeId());
        assertEquals(orderTypeComputed.getDescription(), orderDetails.getOrderTypeName());
        assertEquals(order.getDeliveryDate(), orderDetails.getDeliveryDate());
        assertNull(orderDetails.getAequosId());
        assertTrue(orderDetails.isEditable());
        assertFalse(orderDetails.isExternal());
        assertTrue(orderDetails.isComputedAmount());
        assertEquals(0.0, orderDetails.getShippingCost().doubleValue(), 0.0001);
        assertFalse(orderDetails.isAccounted());
        assertNull(orderDetails.getInvoiceNumber());
        assertNull(orderDetails.getInvoiceAmount());
        assertNull(orderDetails.getInvoiceDate());
        assertFalse(orderDetails.isPaid());
        assertNull(orderDetails.getPaymentDate());
        assertFalse(orderDetails.isSendWeightsRequired());
        assertFalse(orderDetails.isSendWeightsAllowed());
        assertFalse(orderDetails.isHasAttachment());
        assertFalse(orderDetails.isSent());
        assertNull(orderDetails.getExternalOrderId());
        assertNull(orderDetails.getSyncDate());
    }

    @Test
    void givenAnAdmin_whenGettingOrderDetails_thenOrderDetailsAreReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + order.getId(), OrderDetailsDTO.class);
        assertNotNull(orderDetails);
    }

        @Test
    void givenAManagerOfOtherOrderType_whenGettingSummary_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingSummary_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAClosedOrder_whenGettingSummary_thenAllValuesAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        verifyProductDTO(products, "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",  "Birra",
                "PZ", 1.0, 3.65, 3.65, 36.5, -32.85, 3.65, 1.0, 1.0, 10.0, 10.0, 1);

        verifyProductDTO(products, "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",  "Birra",
                "PZ", 1.0, 3.65, 14.6, 29.2, -14.6, 14.6, 4.0, 4.0, 8.0, 8.0, 2);

        verifyProductDTO(products, "MELE1", "MELE CRIMSON CRISP - Roncaglia",  "Frutta",
                "KG", 8.5, 1.55, 13.175, 13.175, 0.0, 13.175, 1.0, 8.5, 1.0, 8.5, 3);

        verifyProductDTO(products, "MELE2", "MELE OPAL - Roncaglia",  "Frutta",
                "KG", 8.5, 1.70, 5.1, 43.35, -38.25, 5.1, 0.353, 3.0, 3.0, 25.5, 2);

        verifyProductDTO(products, "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",  "Ortaggi",
                "KG", 11.0, 1.45, 22.475, 15.95, 6.525, 22.475, 1.409, 15.5, 1.0, 11.0, 2);
    }

    @Test
    void givenAnAdminUser_whenGettingSummary_thenProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(5, products.size());
    }

    @Test
    void givenAnOrderToManageAndAValidProduct_whenGettingProductOrderItems_thenItemsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        assertEquals(3, items.size());

        verifyOrderItem(items.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(items.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(items.get(userId3), 4.0, 4.0, "KG", false);
    }

    @Test
    void givenANotExistingOrder_whenGettingProductOrderItems_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingProduct_whenGettingProductOrderItems_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        List<OrderItemByProductDTO> items = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product/" + UUID.randomUUID(), OrderItemByProductDTO.class);
        assertTrue(items.isEmpty());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingProductOrderItems_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingProductOrderItems_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdmin_whenGettingProductOrderItems_thenItemsAreReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        List<OrderItemByProductDTO> items = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId(), OrderItemByProductDTO.class);
        assertEquals(3, items.size());
    }

    @Test
    void givenAnOrderToManageAndAValidProduct_whenCancellingProductOrder_thenAllItemsAreCancelled() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertTrue(productResponse.isCancelled());
        assertEquals(0.0, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getOrderedQty().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        assertEquals(3, items.size());

        verifyOrderItem(items.get(userId1), 2.5, 0.0, "KG", true);
        verifyOrderItem(items.get(userId2), 2.0, 0.0, "KG", true);
        verifyOrderItem(items.get(userId3), 4.0, 0.0, "KG", true);

        Map<String, OrderItemByProductDTO> otherProductItems = getProductItems(order.getId(), "MELE2");

        assertEquals(2, otherProductItems.size());

        verifyOrderItem(otherProductItems.get(userId1), 1.5, 1.5, "KG", false);
        verifyOrderItem(otherProductItems.get(userId3), 1.5, 1.5, "KG", false);

        verifyUserOrderTotal("user1", 21.86);
        verifyUserOrderTotal("user2", 14.12);
        verifyUserOrderTotal("user3", 9.85);
    }

    @Test
    void givenANotExistingOrder_whenCancellingProductOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/cancel")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenCancellingProductOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/cancel")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenCancellingProductOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/cancel")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledProductOrder_whenRestoringProductOrder_thenAllItemsAreRestoredToPreviousQuantities() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        BasicResponseDTO restoreResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/restore", BasicResponseDTO.class);
        assertEquals("OK", restoreResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertFalse(productResponse.isCancelled());
        assertEquals(13.175, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(0.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        assertEquals(3, items.size());

        verifyOrderItem(items.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(items.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(items.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAModifiedProductOrder_whenRestoringProductOrder_thenAllItemsAreRestoredToPreviousQuantities() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + items.get(userId1).getOrderItemId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + items.get(userId2).getOrderItemId(), BigDecimal.valueOf(3.0), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());

        OrderByProductDTO productBefore = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productBefore);
        assertFalse(productBefore.isCancelled());
        assertEquals(10.85, productBefore.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productBefore.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(10.85, productBefore.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productBefore.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(7.0, productBefore.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsBefore.size());

        verifyOrderItem(itemsBefore.get(userId1), 2.5, 0.0, "KG", true);
        verifyOrderItem(itemsBefore.get(userId2), 2.0, 3.0, "KG", false);
        verifyOrderItem(itemsBefore.get(userId3), 4.0, 4.0, "KG", false);

        BasicResponseDTO restoreResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/restore", BasicResponseDTO.class);
        assertEquals("OK", restoreResponse.getData());

        OrderByProductDTO productAfter = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productAfter);
        assertEquals(13.175, productAfter.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productAfter.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productAfter.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productAfter.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(8.5, productAfter.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(itemsAfter.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenANotExistingOrder_whenRestoringProductOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/restore")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenRestoringProductOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/restore")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenRestoringProductOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/restore")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnOrderToManageAndAValidProduct_whenChangingPriceOfProduct_thenAllItemsAreUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/price", BigDecimal.valueOf(2.0), BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertEquals(17.0, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(17.0, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(17.0, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getOrderedQty().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getDeliveredQty().doubleValue(), 0.001);

        verifyUserOrderTotal("user1", 26.86);
        verifyUserOrderTotal("user2", 18.12);
        verifyUserOrderTotal("user3", 17.85);
    }

    @Test
    void givenANegativeQuantity_whenChangingPriceOfProduct_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/price", BigDecimal.valueOf(-2.65))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("updateProductPrice.price: must be greater than or equal to 0")));
    }

    @Test
    void givenANotExistingOrder_whenChangingPriceOfProduct_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/price", BigDecimal.valueOf(1.0))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenChangingPriceOfProduct_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/price", BigDecimal.valueOf(1.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenChangingPriceOfProduct_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/price", BigDecimal.valueOf(1.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnOrderToManageAndAValidProduct_whenChangingOrderedBoxes_thenProductIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/supplier", BigDecimal.valueOf(5.0), BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertEquals(13.175, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(65.875, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(5.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(42.5, productResponse.getOrderedQty().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getDeliveredQty().doubleValue(), 0.001);
    }

    @Test
    void givenANegativeQuantity_whenChangingOrderedBoxes_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/supplier", BigDecimal.valueOf(-1.0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("updateSupplierOrderQty.boxes: must be greater than or equal to 0")));;
    }

    @Test
    void givenANotExistingOrder_whenChangingOrderedBoxes_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/supplier", BigDecimal.valueOf(1.0))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenChangingOrderedBoxes_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/supplier", BigDecimal.valueOf(1.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenChangingOrderedBoxes_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/supplier", BigDecimal.valueOf(1.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAUserOrderItem_whenChangingQuantity_thenItemAndProductAreUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        changeUserOrderItemQuantity(order.getId(), "MELE1", userId2, 3.0);

        OrderByProductDTO product = getProductOrder(order.getId(), "MELE1");

        assertNotNull(product);
        assertFalse(product.isCancelled());
        assertEquals(14.725, product.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(14.725, product.getTotalAmount().doubleValue(), 0.001);
        assertEquals(9.5, product.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsBefore.size());

        verifyOrderItem(itemsBefore.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(itemsBefore.get(userId2), 2.0, 3.0, "KG", false);
        verifyOrderItem(itemsBefore.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 18.77);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenANegativeQuantity_whenChangingQuantity_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + items.get(userId2).getOrderItemId(), BigDecimal.valueOf(-3.0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("updateQty.qty: must be greater than or equal to 0")));;
    }

    @Test
    void givenAnEmptyQuantity_whenChangingQuantity_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + items.get(userId2).getOrderItemId(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Malformed JSON request")));
    }

    @Test
    void givenANotExistingOrder_whenChangingQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> items = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/item/" + items.get(userId2).getOrderItemId(), BigDecimal.valueOf(3.0))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingItem_whenChangingQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID(), BigDecimal.valueOf(3.0))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenChangingQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID(), BigDecimal.valueOf(3.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenChangingQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID(), BigDecimal.valueOf(3.0))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenMoreOrderedQtyThanDeliveredQty_whenDistributingQuantities_thenAllItemsAreUpdatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderByProductDTO productBefore = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productBefore);
        assertEquals(5.1, productBefore.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productBefore.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(5.1, productBefore.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productBefore.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(3.0, productBefore.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE2");

        assertEquals(2, itemsBefore.size());

        verifyOrderItem(itemsBefore.get(userId1), 1.5, 1.5, "KG", false);
        verifyOrderItem(itemsBefore.get(userId3), 1.5, 1.5, "KG", false);

        BasicResponseDTO distributeResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE2").getId() + "/distribute", BasicResponseDTO.class);
        assertEquals("OK", distributeResponse.getData());

        OrderByProductDTO productAfter = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productAfter);
        assertEquals(43.35, productAfter.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productAfter.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productAfter.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productAfter.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(25.5, productAfter.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE2");

        assertEquals(2, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 1.5, 12.75, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 1.5, 12.75, "KG", false);

        verifyUserOrderTotal("user1", 44.86);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 35.18);
    }

    @Test
    void givenLessOrderedQtyThanDeliveredQty_whenDistributingQuantities_thenAllItemsAreUpdatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderByProductDTO productBefore = getProductOrder(order.getId(), "PATATE");

        assertNotNull(productBefore);
        assertEquals(22.475, productBefore.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(15.95, productBefore.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(22.475, productBefore.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productBefore.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(15.5, productBefore.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "PATATE");

        assertEquals(2, itemsBefore.size());

        verifyOrderItem(itemsBefore.get(userId1), 10.8, 10.8, "KG", false);
        verifyOrderItem(itemsBefore.get(userId2), 4.7, 4.7, "KG", false);

        BasicResponseDTO distributeResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("PATATE").getId() + "/distribute", BasicResponseDTO.class);
        assertEquals("OK", distributeResponse.getData());

        OrderByProductDTO productAfter = getProductOrder(order.getId(), "PATATE");

        assertNotNull(productAfter);
        assertEquals(15.95725, productAfter.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(15.95, productAfter.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(15.95725, productAfter.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productAfter.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(11.005, productAfter.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "PATATE");

        assertEquals(2, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 10.8, 7.668, "KG", false);
        verifyOrderItem(itemsAfter.get(userId2), 4.7, 3.337, "KG", false);

        verifyUserOrderTotal("user1", 21.19);
        verifyUserOrderTotal("user2", 15.24);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenANotExistingOrder_whenDistributingQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/product/" + productsByCodeComputed.get("PATATE").getId() + "/distribute")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingProduct_whenDistributingQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + UUID.randomUUID() + "/distribute")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenDistributingQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("PATATE").getId() + "/distribute")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenDistributingQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("PATATE").getId() + "/distribute")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANewUserOrderItem_whenAddingUserQuantity_thenItemIsAddedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderByProductDTO productBefore = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productBefore);
        assertEquals(5.1, productBefore.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productBefore.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(5.1, productBefore.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productBefore.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(3.0, productBefore.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE2");

        assertEquals(2, itemsBefore.size());

        verifyOrderItem(itemsBefore.get(userId1), 1.5, 1.5, "KG", false);
        verifyOrderItem(itemsBefore.get(userId3), 1.5, 1.5, "KG", false);

        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(userId2);
        orderItemUpdateRequest.setProductId(productsByCodeComputed.get("MELE2").getId());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(2.456));
        orderItemUpdateRequest.setUnitOfMeasure("KG");

        BasicResponseDTO distributeResponse = mockMvcGoGas.postDTO("/api/order/manage/" + order.getId() + "/item", orderItemUpdateRequest, BasicResponseDTO.class);
        assertEquals("OK", distributeResponse.getData());

        OrderByProductDTO productAfter = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productAfter);
        assertEquals(9.2752, productAfter.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productAfter.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(9.2752, productAfter.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productAfter.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(5.456, productAfter.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE2");

        assertEquals(3, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 1.5, 1.5, "KG", false);
        verifyOrderItem(itemsAfter.get(userId2), 0.0, 2.456, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 1.5, 1.5, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 21.39);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAnExistingUserOrderItem_whenAddingUserQuantity_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(userId1);
        orderItemUpdateRequest.setProductId(productsByCodeComputed.get("MELE2").getId());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(2.456));
        orderItemUpdateRequest.setUnitOfMeasure("KG");

        mockMvcGoGas.post("/api/order/manage/" + order.getId() + "/item", orderItemUpdateRequest)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("User item already existing for order and product")));
    }

    @Test
    void givenANotExistingOrder_whenAddingUserQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(userId1);
        orderItemUpdateRequest.setProductId(productsByCodeComputed.get("MELE2").getId());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(2.456));
        orderItemUpdateRequest.setUnitOfMeasure("KG");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/item", orderItemUpdateRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingProduct_whenAddingUserQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(userId1);
        orderItemUpdateRequest.setProductId(UUID.randomUUID().toString());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(2.456));
        orderItemUpdateRequest.setUnitOfMeasure("KG");

        mockMvcGoGas.post("/api/order/manage/" + order.getId() + "/item", orderItemUpdateRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAddingUserQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + order.getId() + "/item", new OrderItemUpdateRequest())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenAddingUserQuantity_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + order.getId() + "/item", new OrderItemUpdateRequest())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingUser_whenAddingUserQuantity_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(UUID.randomUUID().toString());
        orderItemUpdateRequest.setProductId(productsByCodeComputed.get("MELE2").getId());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(2.456));
        orderItemUpdateRequest.setUnitOfMeasure("KG");

        mockMvcGoGas.post("/api/order/manage/" + order.getId() + "/item", orderItemUpdateRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAUserProductItem_whenCancellingUserOrderItem_thenItemAndProductAreUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertEquals(9.3, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(9.3, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(6.0, productResponse.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 2.5, 0.0, "KG", true);
        verifyOrderItem(itemsAfter.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 21.86);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAUserProductItemOfOtherOrder_whenCancellingUserOrderItem_thenErrorIsReturned() throws Exception {
        Order otherOrder = mockOrdersData.createOrder(orderTypeComputed, "2022-02-20", "2022-02-28", "2022-03-01", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + otherOrder.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/cancel")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingOrder_whenCancellingUserOrderItem_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/cancel")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenCancellingUserOrderItem_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/cancel")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenCancellingUserOrderItem_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/cancel")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledItem_whenRestoringProductOrder_thenItemAndProductAreUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        BasicResponseDTO restoreResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/restore", BasicResponseDTO.class);
        assertEquals("OK", restoreResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertEquals(13.175, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(itemsAfter.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAModifiedItem_whenRestoringProductOrder_thenItemAndProductAreUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId(), BigDecimal.valueOf(3.0), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());

        BasicResponseDTO restoreResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/restore", BasicResponseDTO.class);
        assertEquals("OK", restoreResponse.getData());

        OrderByProductDTO productResponse = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponse);
        assertEquals(13.175, productResponse.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponse.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponse.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(8.5, productResponse.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAfter = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsAfter.size());

        verifyOrderItem(itemsAfter.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(itemsAfter.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(itemsAfter.get(userId3), 4.0, 4.0, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenANotExistingOrder_whenRestoringUserOrderItem_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/restore")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenRestoringUserOrderItem_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/restore")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenRestoringUserOrderItem_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/restore")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAUserProductItemReplacedWithNotOrderedOne_whenReplacingProduct_thenItemIsCancelledAndNewOneIsCreated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId2).getOrderItemId() + "/replace", productsByCodeComputed.get("MELE2").getId(), BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponseCancelled = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponseCancelled);
        assertEquals(10.075, productResponseCancelled.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponseCancelled.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(10.075, productResponseCancelled.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponseCancelled.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(6.5, productResponseCancelled.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsCancelled = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsCancelled.size());

        verifyOrderItem(itemsCancelled.get(userId1), 2.5, 2.5, "KG", false);
        verifyOrderItem(itemsCancelled.get(userId2), 2.0, 0.0, "KG", true);
        verifyOrderItem(itemsCancelled.get(userId3), 4.0, 4.0, "KG", false);

        OrderByProductDTO productResponseAdded = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productResponseAdded);
        assertEquals(8.5, productResponseAdded.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productResponseAdded.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(8.5, productResponseAdded.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productResponseAdded.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(5.0, productResponseAdded.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAdded = getProductItems(order.getId(), "MELE2");

        assertEquals(3, itemsAdded.size());

        verifyOrderItem(itemsAdded.get(userId1), 1.5, 1.5, "KG", false);
        verifyOrderItem(itemsAdded.get(userId2), 0.0, 2.0, "KG", false);
        verifyOrderItem(itemsAdded.get(userId3), 1.5, 1.5, "KG", false);

        verifyUserOrderTotal("user1", 25.74);
        verifyUserOrderTotal("user2", 17.52);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAUserProductItemReplacedWithOrderedOne_whenReplacingProduct_thenItemIsCancelledAndExistingIsIncreased() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId1).getOrderItemId() + "/replace", productsByCodeComputed.get("MELE2").getId(), BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());

        OrderByProductDTO productResponseCancelled = getProductOrder(order.getId(), "MELE1");

        assertNotNull(productResponseCancelled);
        assertEquals(9.3, productResponseCancelled.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(13.175, productResponseCancelled.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(9.3, productResponseCancelled.getTotalAmount().doubleValue(), 0.001);
        assertEquals(1.0, productResponseCancelled.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(6.0, productResponseCancelled.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsCancelled = getProductItems(order.getId(), "MELE1");

        assertEquals(3, itemsCancelled.size());

        verifyOrderItem(itemsCancelled.get(userId1), 2.5, 0.0, "KG", true);
        verifyOrderItem(itemsCancelled.get(userId2), 2.0, 2.0, "KG", false);
        verifyOrderItem(itemsCancelled.get(userId3), 4.0, 4.0, "KG", false);

        OrderByProductDTO productResponseAdded = getProductOrder(order.getId(), "MELE2");

        assertNotNull(productResponseAdded);
        assertEquals(9.35, productResponseAdded.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(43.35, productResponseAdded.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(9.35, productResponseAdded.getTotalAmount().doubleValue(), 0.001);
        assertEquals(3.0, productResponseAdded.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(5.5, productResponseAdded.getDeliveredQty().doubleValue(), 0.001);

        Map<String, OrderItemByProductDTO> itemsAdded = getProductItems(order.getId(), "MELE2");

        assertEquals(2, itemsAdded.size());

        verifyOrderItem(itemsAdded.get(userId1), 1.5, 4.0, "KG", false);
        verifyOrderItem(itemsAdded.get(userId3), 1.5, 1.5, "KG", false);

        verifyUserOrderTotal("user1", 26.11);
        verifyUserOrderTotal("user2", 17.22);
        verifyUserOrderTotal("user3", 16.05);
    }

    @Test
    void givenAUserProductItemReplacedWithTheSameProduct_whenReplacingProduct_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId2).getOrderItemId() + "/replace", productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Cannot replace a product with the same product")));
    }

    @Test
    void givenANotExistingOrder_whenReplacingProduct_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + UUID.randomUUID() + "/item/" + itemsBefore.get(userId2).getOrderItemId() + "/replace", productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingItem_whenReplacingProduct_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/replace", productsByCodeComputed.get("MELE1").getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotExistingProduct_whenReplacingProduct_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(order.getId(), "MELE1");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + itemsBefore.get(userId2).getOrderItemId() + "/replace", UUID.randomUUID().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenReplacingProduct_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/replace", UUID.randomUUID().toString())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenReplacingProduct_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + order.getId() + "/item/" + UUID.randomUUID() + "/replace", UUID.randomUUID().toString())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAProductWithUserWithoutOrderItem_whenGettingUsersNotOrdering_thenUsersAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Set<String> userIds = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("BIRRA1").getId() + "/availableUsers", SelectItemDTO.class).stream()
                .map(SelectItemDTO::getId)
                .collect(Collectors.toSet());

        assertTrue(userIds.containsAll(Set.of(userId2, userId3)));
        assertFalse(userIds.contains(userId1));
    }

    @Test
    void givenAProductWithAllUserWithOrderItem_whenGettingUsersNotOrdering_thenNoUsersAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        Set<String> userIds = mockMvcGoGas.getDTOList("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/availableUsers", SelectItemDTO.class).stream()
                .map(SelectItemDTO::getId)
                .collect(Collectors.toSet());

        Set<String> userIdsWithOrder = Set.of(this.userId1, userId2, userId3);
        assertTrue(userIds.stream().noneMatch(userIdsWithOrder::contains));
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingUsersNotOrdering_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/availableUsers")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenGettingUsersNotOrdering_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + order.getId() + "/product/" + productsByCodeComputed.get("MELE1").getId() + "/availableUsers")
                .andExpect(status().isForbidden());
    }

    private void verifyProductDTO(Map<String, OrderByProductDTO> products, String productCode,
                                  String description, String category, String um, double boxWeight, double price,
                                  double deliveredAmount, double orderedAmount, double amountDifference, double totalAmount,
                                  double deliveredBoxes, double deliveredQty, double orderedBoxes, double orderedQty, int usersCount) {

        OrderByProductDTO productDTO = products.get(productsByCodeComputed.get(productCode).getId().toUpperCase());
        assertEquals(price, productDTO.getPrice().doubleValue(), 0.001);
        assertEquals(description, productDTO.getProductName());
        assertEquals(category, productDTO.getCategory());
        assertEquals(um, productDTO.getUnitOfMeasure());
        assertEquals(deliveredAmount, productDTO.getDeliverdTotalAmount().doubleValue(), 0.001);
        assertEquals(orderedAmount, productDTO.getOrderedTotalAmount().doubleValue(), 0.001);
        assertEquals(boxWeight, productDTO.getBoxWeight().doubleValue(), 0.001);
        assertEquals(amountDifference, productDTO.getAmountDifference().doubleValue(), 0.001);
        assertEquals(deliveredBoxes, productDTO.getDeliveredBoxes().doubleValue(), 0.001);
        assertEquals(deliveredQty, productDTO.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(orderedBoxes, productDTO.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(orderedQty, productDTO.getOrderedQty().doubleValue(), 0.001);
        assertEquals(totalAmount, productDTO.getTotalAmount().doubleValue(), 0.001);
        assertEquals(usersCount, productDTO.getOrderingUsersCount());
    }

    private void createUserOrders(Map<String, Map<String, Double>> userQuantities) {
        userQuantities
                .forEach((userId, quantities) -> quantities
                        .forEach((productCode, quantity) ->
                                mockOrdersData.createDeliveredUserOrderItem(order.getId(), userId, productsByCodeComputed.get(productCode), quantity)
                        )
                );

        mockOrdersData.updateUserTotals(order.getId());
    }

    private void verifyUserOrderTotal(String userName, double expectedAmount) throws Exception {
        mockMvcGoGas.loginAs(userName, "password");
        UserOrderDetailsDTO userOrderDetailsDTO = mockMvcGoGas.getDTO("/api/order/user/" + order.getId(), UserOrderDetailsDTO.class, Map.of("includeTotalAmount", List.of("true")));
        assertEquals(expectedAmount, userOrderDetailsDTO.getTotalAmount().doubleValue());
    }
}
