package eu.aequos.gogas.order.user;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.order.OrderBaseIntegrationTest;
import eu.aequos.gogas.persistence.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserOrderIntegrationTest extends OrderBaseIntegrationTest {

    private Order computedOrder;
    private Order notComputedOrder;
    private Order externalOrder;

    @BeforeEach
    void createOrders() {
        computedOrder = mockOrdersData.createOpenOrder(orderTypeComputed);
        notComputedOrder = mockOrdersData.createOpenOrder(orderTypeNotComputed);
        externalOrder = mockOrdersData.createOpenOrder(orderTypeExternal);
    }

    @Test
    void givenAnOpenOrder_whenGettingDetails_thenOrderDetailsAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(computedOrder.getId());
        assertEquals(orderTypeComputed.getId().toUpperCase(), userOrderDetails.getOrderTypeId());
        assertEquals(orderTypeComputed.getDescription(), userOrderDetails.getOrderTypeName());
        assertEquals(computedOrder.getId().toUpperCase(), userOrderDetails.getId());
        assertEquals(LocalDate.now().plusDays(1), userOrderDetails.getDueDate());
        assertEquals(LocalDate.now().plusDays(2), userOrderDetails.getDeliveryDate());
        assertTrue(userOrderDetails.isOpen());
        assertTrue(userOrderDetails.isEditable());
        assertFalse(userOrderDetails.isShowAdvance());
        assertFalse(userOrderDetails.isShowBoxCompletion());
    }

    @Test
    void givenAnOrderWithShowAdvance_whenGettingDetails_thenOrderDetailsAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockOrdersData.forceShowAdvance(orderTypeComputed, true);

        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(computedOrder.getId());
        assertTrue(userOrderDetails.isOpen());
        assertTrue(userOrderDetails.isEditable());
        assertTrue(userOrderDetails.isShowAdvance());
        assertFalse(userOrderDetails.isShowBoxCompletion());

        mockOrdersData.forceShowAdvance(orderTypeComputed, false);
    }

    @Test
    void givenAnOrderWithShowBoxCompletion_whenGettingDetails_thenOrderDetailsAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockOrdersData.forceShowBoxCompletion(orderTypeComputed, true);

        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(computedOrder.getId());
        assertTrue(userOrderDetails.isOpen());
        assertTrue(userOrderDetails.isEditable());
        assertFalse(userOrderDetails.isShowAdvance());
        assertTrue(userOrderDetails.isShowBoxCompletion());

        mockOrdersData.forceShowBoxCompletion(orderTypeComputed, false);
    }

    @Test
    void givenAnExpiredOrder_whenGettingDetails_thenOrderDetailsAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Order expiredOrder = mockOrdersData.createOrder(orderTypeComputed, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), 0, BigDecimal.ZERO);

        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(expiredOrder.getId());
        assertTrue(userOrderDetails.isOpen());
        assertFalse(userOrderDetails.isEditable());
        assertFalse(userOrderDetails.isShowAdvance());
        assertFalse(userOrderDetails.isShowBoxCompletion());
    }

    @Test
    void givenAClosedOrder_whenGettingDetails_thenOrderDetailsAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Order closedOrder = mockOrdersData.createOrder(orderTypeComputed, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), 1, BigDecimal.ZERO);

        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(closedOrder.getId());
        assertFalse(userOrderDetails.isOpen());
        assertFalse(userOrderDetails.isEditable());
        assertFalse(userOrderDetails.isShowAdvance());
        assertFalse(userOrderDetails.isShowBoxCompletion());
    }

    @Test
    void givenAnOpenOrderWithNoItems_whenGettingItemsList_thenAllProductsAreReturnedWithEmptyQuantities() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        List<UserOrderItemDTO> userItems = getUserOrderItems(computedOrder.getId(), userId1);
        assertEquals(6, userItems.size());
        assertTrue(userItems.stream().allMatch(this::itemNotOrdered));
    }

    @Test
    void givenAnOpenOrder_whenGettingUserOrderItem_allInfoIsProvided() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        List<UserOrderItemDTO> userItems = getUserOrderItems(computedOrder.getId(), userId1);

        checkUserItemOpenOrder(userItems.get(0), "ARANCE", "ARANCE - Agrinova Bio",
                "Frutta", "green", "KG", "Cassa", 8.0, 1.10, true, null, "Ordinabili solo a cassa", "MN");

        checkUserItemOpenOrder(userItems.get(1), "MELE1", "MELE CRIMSON CRISP - Roncaglia",
                "Frutta", "green", "KG", "Cassa", 8.5, 1.55, false, null, null, "MN");

        checkUserItemOpenOrder(userItems.get(2), "MELE2", "MELE OPAL - Roncaglia",
                "Frutta", "green", "KG", "Cassa", 8.5, 1.7, false, 2.0, null, "MN");

        checkUserItemOpenOrder(userItems.get(3), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                "Ortaggi", "red", "KG", "Cassa", 11.0, 1.45, false, null, null, "BO");

        checkUserItemOpenOrder(userItems.get(4), "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                "Birra", "white", "PZ", null, 1.0, 3.65, false, null, null, "TN");

        checkUserItemOpenOrder(userItems.get(5), "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                "Birra", "white", "PZ", null, 1.0, 3.65, false, null, null, "TN");
    }

    @Test
    void givenAnOpenOrder_whenGettingCategories_allInfoIsProvided() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        List<CategoryDTO> categories = mockMvcGoGas.getDTOList("/api/order/user/" + computedOrder.getId() + "/categories", CategoryDTO.class);

        assertEquals(3, categories.size());

        CategoryDTO category1 = categories.get(0);
        assertNotNull(category1.getId());
        assertEquals("Frutta", category1.getName());
        assertEquals("green", category1.getColor());
        assertNull(category1.getProducts());

        CategoryDTO category2 = categories.get(1);
        assertNotNull(category2.getId());
        assertEquals("Ortaggi", category2.getName());
        assertEquals("red", category2.getColor());
        assertNull(category2.getProducts());

        CategoryDTO category3 = categories.get(2);
        assertNotNull(category3.getId());
        assertEquals("Birra", category3.getName());
        assertEquals("white", category3.getColor());
        assertNull(category3.getProducts());
    }

    @Test
    void givenAnOpenOrderAndNoUserOrder_whenGettingProductNotOrderedByCategories_allProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        CategoryDTO categoryDTO = mockMvcGoGas.getDTO("/api/order/user/" + computedOrder.getId() + "/categories/" + categoriesComputed.get("Frutta").getId() + "/not-ordered", CategoryDTO.class);

        assertNotNull(categoryDTO.getId());
        assertEquals("Frutta", categoryDTO.getName());
        assertEquals("green", categoryDTO.getColor());
        assertEquals(3, categoryDTO.getProducts().size());

        Map<String, CategoryDTO.SmallProductDTO> products = categoryDTO.getProducts().stream()
                .collect(Collectors.toMap(CategoryDTO.SmallProductDTO::getName, Function.identity()));

        CategoryDTO.SmallProductDTO product1 = products.get("ARANCE - Agrinova Bio");
        assertNotNull(product1.getId());
        assertEquals("KG", product1.getUm());
        assertEquals("Cassa", product1.getBoxUm());
        assertEquals(1.10, product1.getPrice().doubleValue(), 0.001);

        CategoryDTO.SmallProductDTO product2 = products.get("MELE CRIMSON CRISP - Roncaglia");
        assertNotNull(product2.getId());
        assertEquals("KG", product2.getUm());
        assertEquals("Cassa", product2.getBoxUm());
        assertEquals(1.55, product2.getPrice().doubleValue(), 0.001);

        CategoryDTO.SmallProductDTO product3 = products.get("MELE OPAL - Roncaglia");
        assertNotNull(product3.getId());
        assertEquals("KG", product3.getUm());
        assertEquals("Cassa", product3.getBoxUm());
        assertEquals(1.70, product3.getPrice().doubleValue(), 0.001);
    }

    @Test
    void givenAnOpenOrderAndOneUserOrder_whenGettingProductNotOrderedByCategories_allOtherProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        sendUserOrderItem(computedOrder.getId(), userId1, productsByCodeComputed.get("ARANCE").getId(), 1.0, "Cassa");

        CategoryDTO categoryDTO = mockMvcGoGas.getDTO("/api/order/user/" + computedOrder.getId() + "/categories/" + categoriesComputed.get("Frutta").getId() + "/not-ordered", CategoryDTO.class);

        assertNotNull(categoryDTO.getId());
        assertEquals("Frutta", categoryDTO.getName());
        assertEquals("green", categoryDTO.getColor());
        assertEquals(2, categoryDTO.getProducts().size());

        Map<String, CategoryDTO.SmallProductDTO> products = categoryDTO.getProducts().stream()
                .collect(Collectors.toMap(CategoryDTO.SmallProductDTO::getName, Function.identity()));

        //check products category
        CategoryDTO.SmallProductDTO product2 = products.get("MELE CRIMSON CRISP - Roncaglia");
        assertNotNull(product2.getId());
        assertEquals("KG", product2.getUm());
        assertEquals("Cassa", product2.getBoxUm());
        assertEquals(1.55, product2.getPrice().doubleValue(), 0.001);

        CategoryDTO.SmallProductDTO product3 = products.get("MELE OPAL - Roncaglia");
        assertNotNull(product3.getId());
        assertEquals("KG", product3.getUm());
        assertEquals("Cassa", product3.getBoxUm());
        assertEquals(1.70, product3.getPrice().doubleValue(), 0.001);
    }

    @Test
    void givenAnInvalidOrder_whenGettingCategories_notFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/user/" + UUID.randomUUID() + "/categories")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnInvalidOrder_whenGettingProductNotOrderedByCategories_notFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/user/" + UUID.randomUUID() + "/categories/" + categoriesComputed.get("Frutta").getId() + "/not-ordered")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenACategoryBelongingToAnotherOrderType_whenGettingProductNotOrderedByCategories_notFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/user/" + computedOrder.getId() + "/categories/" + categoriesNotComputed.get("Carne Fresca").getId() + "/not-ordered")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnInvalidCategory_whenGettingProductNotOrderedByCategories_notFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/user/" + computedOrder.getId() + "/categories/" + UUID.randomUUID() + "/not-ordered")
                .andExpect(status().isNotFound());
    }

    private void checkUserItemOpenOrder(UserOrderItemDTO userItem, String productCode, String productName,
                                        String categoryName, String categoryColor, String um, String boxUm, double boxWeight,
                                        double price, boolean boxOnly, Double orderMultiple, String productNotes, String productProvince) {

        assertEquals(productsByCodeComputed.get(productCode).getId().toUpperCase(), userItem.getProductId());
        assertEquals(productName, userItem.getProductName());
        assertEquals(categoryName, userItem.getCategory());
        assertEquals(categoryColor, userItem.getCategoryColor());
        assertEquals(um, userItem.getUnitOfMeasure());
        assertEquals(boxUm, userItem.getBoxUnitOfMeasure());
        assertEquals(boxWeight, userItem.getBoxWeight().doubleValue(), 0.001);
        assertEquals(price, userItem.getUnitPrice().doubleValue(), 0.001);
        assertEquals(boxOnly, userItem.isBoxesOnly());
        assertEquals(orderMultiple, Optional.ofNullable(userItem.getOrderMultiple()).map(BigDecimal::doubleValue).orElse(null));
        assertEquals(productNotes, userItem.getProductNotes());
        assertEquals(productProvince, userItem.getProductProvince());
    }

    @Test
    void givenAClosedOrder_whenGettingUserOrderItem_onlyOrderedItemsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        sendUserOrderItem(computedOrder.getId(), userId1, productsByCodeComputed.get("ARANCE").getId(), 1.0, "Cassa");
        sendUserOrderItem(computedOrder.getId(), userId1, productsByCodeComputed.get("MELE1").getId(), 1.5, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        sendUserOrderItem(computedOrder.getId(), userId2, productsByCodeComputed.get("MELE2").getId(), 1.0, "Cassa");
        sendUserOrderItem(computedOrder.getId(), userId2, productsByCodeComputed.get("PATATE").getId(), 3.0, "KG");

        mockMvcGoGas.loginAs("user3", "password");
        sendUserOrderItem(computedOrder.getId(), userId3, productsByCodeComputed.get("MELE1").getId(), 4.0, "KG");
        sendUserOrderItem(computedOrder.getId(), userId3, productsByCodeComputed.get("PATATE").getId(), 5.0, "KG");

        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("user1", "password");
        List<UserOrderItemDTO> userItems1 = getUserOrderItems(computedOrder.getId(), userId1);

        assertEquals(2, userItems1.size());

        checkUserItemClosedOrder(userItems1.get(0), "ARANCE", "ARANCE - Agrinova Bio", 8.0, "KG",
                "Frutta", "green", "KG", "Cassa", 8.0, 1.10, true, null, "Ordinabili solo a cassa", "MN");

        checkUserItemClosedOrder(userItems1.get(1), "MELE1", "MELE CRIMSON CRISP - Roncaglia", 1.5, "KG",
                "Frutta", "green", "KG", "Cassa", 8.5, 1.55, false, null, null, "MN");

        mockMvcGoGas.loginAs("user2", "password");
        List<UserOrderItemDTO> userItems2 = getUserOrderItems(computedOrder.getId(), userId2);

        assertEquals(2, userItems2.size());

        checkUserItemClosedOrder(userItems2.get(0), "MELE2", "MELE OPAL - Roncaglia", 8.5, "KG",
                "Frutta", "green", "KG", "Cassa", 8.5, 1.7, false, 2.0, null, "MN");

        checkUserItemClosedOrder(userItems2.get(1), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio", 3.0, "KG",
                "Ortaggi", "red", "KG", "Cassa", 11.0, 1.45, false, null, null, "BO");

        mockMvcGoGas.loginAs("user3", "password");
        List<UserOrderItemDTO> userItems3 = getUserOrderItems(computedOrder.getId(), userId3);

        assertEquals(2, userItems3.size());

        checkUserItemClosedOrder(userItems3.get(0), "MELE1", "MELE CRIMSON CRISP - Roncaglia", 4.0, "KG",
                "Frutta", "green", "KG", "Cassa", 8.5, 1.55, false, null, null, "MN");

        checkUserItemClosedOrder(userItems3.get(1), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio", 5.0, "KG",
                "Ortaggi", "red", "KG", "Cassa", 11.0, 1.45, false, null, null, "BO");
    }

    private void checkUserItemClosedOrder(UserOrderItemDTO userItem, String productCode, String productName, double qta, String orderUm,
                                          String categoryName, String categoryColor, String um, String boxUm, double boxWeight,
                                          double price, boolean boxOnly, Double orderMultiple, String productNotes, String productProvince) {

        assertEquals(productsByCodeComputed.get(productCode).getId().toUpperCase(), userItem.getProductId());
        assertEquals(productName, userItem.getProductName());
        assertEquals(qta, userItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals(qta, userItem.getOrderDeliveredQty().doubleValue(), 0.001);
        assertEquals(orderUm, userItem.getOrderUnitOfMeasure());
        assertEquals(categoryName, userItem.getCategory());
        assertEquals(categoryColor, userItem.getCategoryColor());
        assertEquals(um, userItem.getUnitOfMeasure());
        assertEquals(boxUm, userItem.getBoxUnitOfMeasure());
        assertEquals(boxWeight, userItem.getBoxWeight().doubleValue(), 0.001);
        assertEquals(price, userItem.getUnitPrice().doubleValue(), 0.001);
        assertEquals(boxOnly, userItem.isBoxesOnly());
        assertEquals(orderMultiple, Optional.ofNullable(userItem.getOrderMultiple()).map(BigDecimal::doubleValue).orElse(null));
        assertEquals(productNotes, userItem.getProductNotes());
        assertEquals(productProvince, userItem.getProductProvince());
    }

    @Test
    void givenAValidOrderUpdate_whenAddingUserOrderItem_itemIsAdded() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 1.0, "KG");
        assertEquals(1, updatedOrderItem.getItemsAdded());
        assertEquals(1.0, updatedOrderItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertNull(updatedOrderItem.getOrderDeliveredQty());
        assertEquals("KG", updatedOrderItem.getOrderUnitOfMeasure());
        assertEquals("Cassa", updatedOrderItem.getBoxUnitOfMeasure());
        assertEquals(8.5, updatedOrderItem.getBoxWeight().doubleValue(), 0.001);
        assertEquals(1.55, updatedOrderItem.getUnitPrice().doubleValue(), 0.001);
        assertEquals(1.55, updatedOrderItem.getOrderTotalAmount().doubleValue(), 0.001);

        List<UserOrderItemDTO> userItems = getUserOrderItems(computedOrder.getId(), userId1);
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

        checkOrderTotalAmount(computedOrder.getId(), 1.55);
    }

    @Test
    void givenAValidOrderUpdate_whenAddingUserOrderItem_productStatsAreUpdated() throws Exception {
        String productId = productsByCodeComputed.get("MELE1").getId();

        mockMvcGoGas.loginAs("user1", "password");
        SmallUserOrderItemDTO updatedOrderItem1 = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.5, "KG");
        assertEquals(2.5, updatedOrderItem1.getProductTotalOrderedQty().doubleValue());
        assertEquals(0, updatedOrderItem1.completeBoxesCount());
        assertEquals(2.5, updatedOrderItem1.boxCompletedUnits().doubleValue());
        assertEquals(6.0, updatedOrderItem1.boxAvailableUnits().doubleValue());

        mockMvcGoGas.loginAs("user2", "password");
        SmallUserOrderItemDTO updatedOrderItem2 = sendUserOrderItem(computedOrder.getId(), userId2, productId, 3.0, "KG");
        assertEquals(5.5, updatedOrderItem2.getProductTotalOrderedQty().doubleValue());
        assertEquals(0, updatedOrderItem2.completeBoxesCount());
        assertEquals(5.5, updatedOrderItem2.boxCompletedUnits().doubleValue());
        assertEquals(3.0, updatedOrderItem2.boxAvailableUnits().doubleValue());

        mockMvcGoGas.loginAs("user3", "password");
        SmallUserOrderItemDTO updatedOrderItem3 = sendUserOrderItem(computedOrder.getId(), userId3, productId, 1.0, "Cassa");
        assertEquals(14.0, updatedOrderItem3.getProductTotalOrderedQty().doubleValue());
        assertEquals(1, updatedOrderItem3.completeBoxesCount());
        assertEquals(5.5, updatedOrderItem3.boxCompletedUnits().doubleValue());
        assertEquals(3.0, updatedOrderItem3.boxAvailableUnits().doubleValue());
    }

    @Test
    void givenAOrderUpdate_whenUpdatingUserOrderItem_itemIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());
        assertEquals(2.0, addOrderItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals("KG", addOrderItem.getOrderUnitOfMeasure());
        assertEquals(3.10, addOrderItem.getOrderTotalAmount().doubleValue(), 0.001);

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 1.0, "Cassa");
        assertEquals(0, updatedOrderItem.getItemsAdded());
        assertEquals(1.0, updatedOrderItem.getOrderRequestedQty().doubleValue(), 0.001);
        assertEquals("Cassa", updatedOrderItem.getOrderUnitOfMeasure());
        assertEquals(1.55 * 8.5, updatedOrderItem.getOrderTotalAmount().doubleValue(), 0.001);
    }

    @Test
    void givenAOrderUpdateWithZeroQuantity_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 0.0, "KG");
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenAOrderUpdateWithZeroQuantityAndNoUnitOfMeasure_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 0.0, null);
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenAnEmptyQuantity_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, null, "KG");
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenAnEmptyQuantityAndEmptyUnitOfMeasure_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, null, "KG");
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenAZeroQuantityAndEmptyUnitOfMeasureWithBoxOnly_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("ARANCE").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "Cassa");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 0.0, null);
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenEmptyQuantityAndEmptyUnitOfMeasureWithBoxOnly_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("ARANCE").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "Cassa");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, null, null);
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenAZeroQuantityAndEmptyUnitOfMeasureWithMultiple_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE2").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 0.0, null);
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenEmptyQuantityAndEmptyUnitOfMeasureWithMultiple_whenUpdatingUserOrderItem_itemIsRemoved() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE2").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());

        SmallUserOrderItemDTO updatedOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, null, null);
        assertEquals(-1, updatedOrderItem.getItemsAdded());
        assertNull(updatedOrderItem.getOrderRequestedQty());
        assertNull(updatedOrderItem.getOrderTotalAmount());
    }

    @Test
    void givenANegativeQuantity_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, -2.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnInvalidUM_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 2.0, "Invalid");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Unit of measure not valid")));
    }

    @Test
    void givenAnInvalidProductId_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, UUID.randomUUID().toString(), 2.0, "Invalid");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAProductIdOfAnotherOrderType_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeNotComputed.get("FETTINE").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Product not available for given order")));
    }

    @Test
    void givenAnInvalidOrder_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + UUID.randomUUID() + "/item", request)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnInvalidUser_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(UUID.randomUUID().toString(), productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenADifferentUser_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId2, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnEmptyUser_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(null, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyProduct_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, null, 2.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyUM_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 2.0, null);
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAClosedOrder_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        closeOrder(computedOrder.getId());

        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 2.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message", is("The operation is not allowed, order closed")));
    }

    @Test
    void givenAnExpiredOrder_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockOrdersData.forceOrderDates(computedOrder.getId(), LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE1").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 2.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message", is("The operation is not allowed, order closed")));
    }

    @Test
    void givenAQuantityNotMatchingMultiple_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE2").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 1.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Prodotto ordinabile a multipli di 2.00")));
    }

    @Test
    void givenAQuantityNotMatchingBoxOnly_whenUpdatingUserOrderItem_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("ARANCE").getId();

        OrderItemUpdateRequest request = buildOrderItemUpdateRequest(userId1, productId, 2.0, "KG");
        mockMvcGoGas.post("/api/order/user/" + computedOrder.getId() + "/item", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Prodotto ordinabile solo a collo intero")));
    }

    @Test
    void givenAQuantityMatchingMultiple_whenUpdatingUserOrderItem_thenItemIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE2").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 2.0, "KG");
        assertEquals(1, addOrderItem.getItemsAdded());
    }

    @Test
    void givenAQuantityNotMatchingMultipleButForBoxUm_whenUpdatingUserOrderItem_thenItemIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("MELE2").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 1.0, "Cassa");
        assertEquals(1, addOrderItem.getItemsAdded());
    }

    @Test
    void givenAQuantityMatchingBoxOnly_whenUpdatingUserOrderItem_thenItemIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String productId = productsByCodeComputed.get("ARANCE").getId();

        SmallUserOrderItemDTO addOrderItem = sendUserOrderItem(computedOrder.getId(), userId1, productId, 1.0, "Cassa");
        assertEquals(1, addOrderItem.getItemsAdded());
    }

    private void checkOrderTotalAmount(String orderId, double expectedAmount) throws Exception {
        UserOrderDetailsDTO userOrderDetails = getUserOrderDetails(orderId);
        assertEquals(expectedAmount, userOrderDetails.getTotalAmount().doubleValue(), 0.001);
    }

    private UserOrderDetailsDTO getUserOrderDetails(String orderId) throws Exception {
        return mockMvcGoGas.getDTO("/api/order/user/" + orderId, UserOrderDetailsDTO.class, Map.of("includeTotalAmount", List.of("true")));
    }

    private List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/user/" + orderId + "/items", UserOrderItemDTO.class, Map.of("userId", List.of(userId)));
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
}
