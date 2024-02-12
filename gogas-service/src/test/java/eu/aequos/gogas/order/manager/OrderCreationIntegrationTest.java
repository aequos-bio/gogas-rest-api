package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.dto.OrderDetailsDTO;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderCreationIntegrationTest extends OrderManagementBaseIntegrationTest {

    @Test
    void givenAValidOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(),
                 null, false, null, true, false);
    }

    @Test
    void givenAValidOrderType_whenCreatingOrder_thenNotificationIsSentCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        createOrder(orderDTO);

        verify(telegramNotificationClient).sendNotifications(eq("integration-test"), argThat(request -> request.getUserIds().size() == 10));
        verify(pushNotificationClient).sendNotifications(any(), argThat(request -> request.getUserIds().size() == 10));
    }

    @Test
    void givenBlacklistForOrderType_whenCreatingOrder_thenNotificationIsNotSentToBlacklistedUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockOrdersData.addBlacklist(userId1, orderTypeComputed);
        createOrder(orderDTO);

        verify(telegramNotificationClient).sendNotifications(eq("integration-test"), argThat(request -> !request.getUserIds().contains(userId1)));
        verify(pushNotificationClient).sendNotifications(any(), argThat(request -> !request.getUserIds().contains(userId1)));
    }

    @Test
    void givenAManagerOfOtherOrderType_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenCreatingOrderWithAdminLogin_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        OrderDTO orderDTO = buildValidOrderDTO(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAValidOrderTypeNotComputed_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeNotComputed.getId().toUpperCase(), orderTypeNotComputed.getDescription(), null, false,
                null, false, false);
    }

    @Test
    void givenAManagerOfOtherOrderTypeComputed_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnExternalOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeExternal.getId(), "aValidUrl");

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeExternal.getId().toUpperCase(), orderTypeExternal.getDescription(), null,
                true, "aValidUrl", false, false);
    }

    @Test
    void givenAnAequosOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeAequos.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeAequos.getId().toUpperCase(), orderTypeAequos.getDescription(), 0,
                false, null, false, true);
    }

    @Test
    void givenOpeningDateInTheFuture_whenCreatingOrder_thenOrderIsCreatedCorrectlyAndActionsIncludeDelete() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4), 10, LocalDate.now().plusDays(6), null);

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(), null,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), 10, LocalDate.now().plusDays(6), "modifica,elimina",
                false, null, true, false);
    }

    @Test
    void givenOpenedOrderWithoutUserItems_whenDeletingOrder_thenOrderIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        BasicResponseDTO creationResponse = mockMvcGoGas.deleteDTO("/api/order/manage/" + orderId, BasicResponseDTO.class);
        assertEquals("OK", creationResponse.getData());

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeComputed.getId());

        assertTrue(mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .noneMatch(order -> order.getId().equals(orderId.toUpperCase())));

        createdOrderIds.remove(orderId);
    }

    @Test
    void givenOpenedOrderWithUserItems_whenDeletingOrder_thenOrderNotIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        addComputedUserOrder(orderId, orderManagerId1, "MELE1", 1.0, "KG");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("L'elemento non può essere eliminato")));
    }

    @Test
    void givenAManagerOfOtherOrderType_whenDeletingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenDeletingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenDeletingOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.delete("/api/order/manage/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenDueDateInThePast_whenCreatingOrder_thenDateIsAccepted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isOk());
    }

    @Test
    void givenDueDateEqualToOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(2), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDueDateBeforeOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(1), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateEqualsToDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(5), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateBeforeDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenEmptyOrderTypeId_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(null, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenEmptyOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), null,
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                null, 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDeliveryDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, null, null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenSecondOrderWithSameDueAndDeliveryDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(10), null);

        createOrder(orderDTO);

        OrderDTO duplicatedOrderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(10), null);

        mockMvcGoGas.post("/api/order/manage", duplicatedOrderDTO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Esiste già un ordine nello stesso periodo")));
    }

    @Test
    void givenAValidOrderType_whenUpdatingOrder_thenOrderIsUpdatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(5), 12, LocalDate.now().plusDays(15), "anUrl");

        updateOrder(orderId, updateOrderDTO);

        verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(), null,
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(5), 12, LocalDate.now().plusDays(15),
                "modifica,dettaglio,cancel", false, "anUrl", true, false);
    }

    @Test
    void givenDueDateInThePast_whenUpdatingOrder_thenDateIsAccepted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isOk());
    }

    @Test
    void givenDueDateEqualToOpeningDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(2), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDueDateBeforeOpeningDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(1), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateEqualsToDueDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(5), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateBeforeDueDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenEmptyOpeningDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, null,
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDueDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(2),
                null, 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDeliveryDate_whenUpdatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, null, null);

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenUpdatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(5), 12, LocalDate.now().plusDays(15), "anUrl");

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenUpdatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO createOrderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(createOrderDTO);

        OrderDTO updateOrderDTO = buildOrderDTO(null, LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(5), 12, LocalDate.now().plusDays(15), "anUrl");

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/order/manage/" + orderId, updateOrderDTO)
                .andExpect(status().isForbidden());
    }

    private void updateOrder(String orderId, OrderDTO orderDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId, orderDTO, BasicResponseDTO.class);
        assertEquals(orderId, creationResponse.getData());
    }

    private void verifyCreatedOrder(String orderId, String orderTypeId, String description, Integer aequosId, boolean external,
                                    String externalUrl, boolean computedAmount, boolean isSendWeightRequired) throws Exception {

        verifyCreatedOrder(orderId, orderTypeId, description, aequosId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 14, LocalDate.now().plusDays(10),
                "modifica,dettaglio,cancel", external, externalUrl, computedAmount, isSendWeightRequired);
    }

    private void verifyCreatedOrder(String orderId, String orderTypeId, String description, Integer aequosId,
                                    LocalDate openingDate, LocalDate dueDate, int dueHours, LocalDate deliveryDate, String actions,
                                    boolean external, String externalUrl, boolean computedAmount, boolean isSendWeightRequired) throws Exception {

        verifyOrder(orderId, orderTypeId, description, openingDate, dueDate, dueHours, deliveryDate, aequosId, 0, "Aperto",
                actions, 0, 0.0, false, external, externalUrl, computedAmount, 0.0, false, isSendWeightRequired, false);
    }

    private void verifyOrder(String orderId, String orderTypeId, String description, LocalDate openingDate, LocalDate dueDate, int dueHours, LocalDate deliveryDate,
                             Integer aequosId, int statusCode, String statusName, String actions, int itemsCount, double totalAmount, boolean editable, boolean external,
                             String externalUrl, boolean computedAmount, double shippingCosts, boolean accounted, boolean isSendWeightRequired, boolean isSendWeightAllowed) throws Exception {

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(orderTypeId, orderDTO.getOrderTypeId());
        assertEquals(description, orderDTO.getOrderTypeName());
        assertEquals(openingDate, orderDTO.getOpeningDate());
        assertEquals(dueDate, orderDTO.getDueDate());
        assertEquals(dueHours, orderDTO.getDueHour());
        assertEquals(deliveryDate, orderDTO.getDeliveryDate());
        assertEquals(statusName, orderDTO.getStatusName());
        assertEquals(statusCode, orderDTO.getStatusCode());
        assertEquals(externalUrl, orderDTO.getExternalLink());
        assertEquals(actions, orderDTO.getActions());
        assertEquals(itemsCount, orderDTO.getItemsCount());
        assertEquals(totalAmount, orderDTO.getTotalAmount().doubleValue(), 0.001);
        assertNull(orderDTO.getInvoiceAmount());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);

        assertNotNull(orderDetails);
        assertEquals(orderTypeId, orderDetails.getOrderTypeId());
        assertEquals(description, orderDetails.getOrderTypeName());
        assertEquals(deliveryDate, orderDetails.getDeliveryDate());
        assertEquals(aequosId, orderDetails.getAequosId());
        assertEquals(editable, orderDetails.isEditable());
        assertEquals(external, orderDetails.isExternal());
        assertEquals(computedAmount, orderDetails.isComputedAmount());
        assertEquals(shippingCosts, orderDetails.getShippingCost().doubleValue(), 0.0001);
        assertEquals(accounted, orderDetails.isAccounted());
        assertNull(orderDetails.getInvoiceNumber());
        assertNull(orderDetails.getInvoiceAmount());
        assertNull(orderDetails.getInvoiceDate());
        assertFalse(orderDetails.isPaid());
        assertNull(orderDetails.getPaymentDate());
        assertEquals(isSendWeightRequired, orderDetails.isSendWeightsRequired());
        assertEquals(isSendWeightAllowed, orderDetails.isSendWeightsAllowed());
        assertFalse(orderDetails.isHasAttachment());
        assertFalse(orderDetails.isSent());
        assertNull(orderDetails.getExternalOrderId());
        assertNull(orderDetails.getSyncDate());
    }
}
