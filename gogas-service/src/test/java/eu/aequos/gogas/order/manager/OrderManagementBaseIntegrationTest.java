package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.order.OrderBaseIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderManagementBaseIntegrationTest extends OrderBaseIntegrationTest {

    protected void performAction(String orderId, String action) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/" + action, null, BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    void performClose(String orderId, int roundType) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/close", null, BasicResponseDTO.class, Map.of("roundType", List.of(String.valueOf(roundType))));
        assertEquals("OK", cancelResponse.getData());
    }

    void invalidAction(String orderId, String action) throws Exception {
        invalidAction(orderId, action, "Invalid order status");
    }

    void invalidAction(String orderId, String action, String errorMessage) throws Exception {
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/" + action, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(errorMessage)));
    }

    List<UserOrderItemDTO> getUserOpenOrderItems(String orderId, String userId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/user/" + orderId + "/items", UserOrderItemDTO.class, Map.of("userId", List.of(userId))).stream()
                .filter(item -> item.getOrderRequestedQty() != null)
                .collect(Collectors.toList());
    }

    protected void addComputedUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        addUserOrder(orderId, userId, productsByCodeComputed.get(productCode).getId(), qty, um);
    }

    void addNotComputedUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        addUserOrder(orderId, userId, productsByCodeNotComputed.get(productCode).getId(), qty, um);
    }

    private void addUserOrder(String orderId, String userId, String productId, double qty, String um) throws Exception {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productId);
        request.setQuantity(BigDecimal.valueOf(qty));
        request.setUnitOfMeasure(um);

        mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    void setUserCost(String orderId, String userId, double amount) throws Exception {
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/byUser/" + userId, BigDecimal.valueOf(amount), BasicResponseDTO.class);
        assertNotNull(updateResponse);
    }

    protected OrderDTO buildValidOrderDTO(String orderTypeId) {
        return buildValidOrderDTO(orderTypeId, null);
    }

    OrderDTO buildValidOrderDTO(String orderTypeId, String externalUrl) {
        return buildOrderDTO(orderTypeId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 14,
                LocalDate.now().plusDays(10), externalUrl);
    }

    OrderDTO buildOrderDTO(String orderTypeId, LocalDate openingDate, LocalDate dueDate, int dueHour,
                                   LocalDate deliveryDate, String externalUrl) {

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderTypeId(orderTypeId);
        orderDTO.setOpeningDate(openingDate);
        orderDTO.setDueDate(dueDate);
        orderDTO.setDueHour(dueHour);
        orderDTO.setDeliveryDate(deliveryDate);
        orderDTO.setExternalLink(externalUrl);
        return orderDTO;
    }

    protected String createOrder(OrderDTO orderDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.postDTO("/api/order/manage", orderDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        String orderId = creationResponse.getData().toString();
        createdOrderIds.add(orderId);
        return orderId;
    }

    void verifyOrderStatus(String orderId, String orderTypeId, int statusCode) throws Exception {
        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(statusCode, orderDTO.getStatusCode());
    }

    void verifyOrderStatusAndActions(String orderId, String orderTypeId, int statusCode, String statusName, String actions,
                             boolean editable, boolean accounted) throws Exception {

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(statusName, orderDTO.getStatusName());
        assertEquals(statusCode, orderDTO.getStatusCode());
        assertEquals(actions, orderDTO.getActions());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);

        assertNotNull(orderDetails);
        assertEquals(editable, orderDetails.isEditable());
        assertEquals(accounted, orderDetails.isAccounted());
    }

    Map<String, OrderItemByProductDTO> getProductItems(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product/" + productsByCodeComputed.get(productCode).getId(), OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    Map<String, OrderItemByProductDTO> getNotComputedProductItems(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product/" + productsByCodeNotComputed.get(productCode).getId(), OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    OrderByProductDTO getProductOrder(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .filter(product -> product.getProductId().equals(productsByCodeComputed.get(productCode).getId().toUpperCase()))
                .findFirst()
                .orElse(null);
    }

    void changeUserOrderItemQuantity(String orderId, String productCode, String userId, double quantity) throws Exception {
        Map<String, OrderItemByProductDTO> items = getProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + items.get(userId).getOrderItemId(), BigDecimal.valueOf(quantity), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void addNewUserOrderItem(String orderId, String productCode, String userId, double quantity, String um) throws Exception {
        OrderItemUpdateRequest orderItemUpdateRequest = new OrderItemUpdateRequest();
        orderItemUpdateRequest.setUserId(userId);
        orderItemUpdateRequest.setProductId(productsByCodeComputed.get(productCode).getId());
        orderItemUpdateRequest.setQuantity(BigDecimal.valueOf(quantity));
        orderItemUpdateRequest.setUnitOfMeasure(um);

        BasicResponseDTO distributeResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/item", orderItemUpdateRequest, BasicResponseDTO.class);
        assertEquals("OK", distributeResponse.getData());

    }

    void changeUserOrderItemQuantityNotComputed(String orderId, String productCode, String userId, double quantity) throws Exception {
        Map<String, OrderItemByProductDTO> items = getNotComputedProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + items.get(userId).getOrderItemId(), BigDecimal.valueOf(quantity), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void cancelProductOrder(String orderId, String productCode) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/product/" + productsByCodeComputed.get(productCode).getId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    void cancelUserOrder(String orderId, String userId, String productCode) throws Exception {
        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + itemsBefore.get(userId).getOrderItemId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void verifyOrderItem(OrderItemByProductDTO item, double requestedQty, double deliveredQty, String um, boolean cancelled) {
        assertEquals(um, item.getUnitOfMeasure());
        assertEquals(requestedQty, item.getRequestedQty().doubleValue(), 0.001);
        assertEquals(deliveredQty, item.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(cancelled, item.isCancelled());
    }

    void verifyOrderOpenedItem(OrderItemByProductDTO item, double requestedQty, String um, boolean cancelled) {
        assertEquals(um, item.getUnitOfMeasure());
        assertEquals(requestedQty, item.getRequestedQty().doubleValue(), 0.001);
        assertNull(item.getDeliveredQty());
        assertEquals(cancelled, item.isCancelled());
    }
}
