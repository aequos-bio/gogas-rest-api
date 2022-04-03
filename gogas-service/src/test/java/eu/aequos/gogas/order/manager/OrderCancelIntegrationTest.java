package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.dto.UserOrderItemDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderCancelIntegrationTest extends OrderManagementBaseIntegrationTest {

    @Test
    void givenAnUnknownAction_whenPerformingActionsOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/unknown", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid action: unknown")));
    }

    @Test
    void givenAnOpenOrderInTheFuture_whenCheckingActionsOrder_thenOnlyCancelActionIsAllowed() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), 10,
                LocalDate.now().plusDays(4), null);

        String orderId = createOrder(orderDTO);

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,elimina", false, false);
    }

    @Test
    void givenAnOpenOrder_whenCheckingActionsOrder_thenOnlyCancelActionIsAllowed() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel", false, false);
    }

    @Test
    void givenAnEmptyOpenOrder_whenCancellingOrder_thenOrderIsCancelled() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        performAction(orderId, "cancel");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 3, "Annullato", "undocancel", false, false);
    }

    @Test
    void givenAnOpenOrderWithUserItems_whenCancellingOrder_thenOrderIsCancelled() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addUserOrder(orderId, orderManagerId1, "MELE1", 1.0, "KG");
        addUserOrder(orderId, orderManagerId1, "MELE2", 2.0, "KG");

        List<UserOrderItemDTO> userItemsBefore = getUserOpenOrderItems(orderId, orderManagerId1);
        assertEquals(2, userItemsBefore.size());

        performAction(orderId, "cancel");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 3, "Annullato", "undocancel", false, false);

        List<UserOrderItemDTO> userItemsAfter = getUserOpenOrderItems(orderId, orderManagerId1);
        assertTrue(userItemsAfter.isEmpty());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenCancellingOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.loginAs("manager", "password");
        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("manager2", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/cancel", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenCancellingOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        mockMvcGoGas.loginAs("manager", "password");
        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("user1", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/cancel", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnClosedOrder_whenCancellingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        invalidAction(orderId, "cancel");
    }

    @Test
    void givenAnAccountedOrder_whenCancellingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        invalidAction(orderId, "cancel");
    }

    @Test
    void givenAnAlreadyCancelledOrder_whenCancellingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        performAction(orderId, "cancel");

        invalidAction(orderId, "cancel");
    }

    @Test
    void givenAnEmptyCancelledOrder_whenUndoingCancel_thenOrderIsOpened() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        performAction(orderId, "cancel");
        performAction(orderId, "undocancel");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel", false, false);
    }

    @Test
    void givenANotEmptyCancelledOrder_whenUndoingCancel_thenOrderIsRestored() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addUserOrder(orderId, orderManagerId1, "MELE1", 1.0, "KG");
        addUserOrder(orderId, orderManagerId1, "MELE2", 2.0, "KG");

        performAction(orderId, "cancel");
        performAction(orderId, "undocancel");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 0, "Aperto", "modifica,dettaglio,cancel", false, false);

        List<UserOrderItemDTO> userItemsBefore = getUserOpenOrderItems(orderId, orderManagerId1);
        assertEquals(2, userItemsBefore.size());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenUndoingCancelOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.loginAs("manager", "password");
        String orderId = createOrder(orderDTO);
        performAction(orderId, "cancel");

        mockMvcGoGas.loginAs("manager2", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/cancel", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenUndoingCancelOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        mockMvcGoGas.loginAs("manager", "password");
        String orderId = createOrder(orderDTO);
        performAction(orderId, "cancel");

        mockMvcGoGas.loginAs("user1", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/cancel", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnClosedOrder_whenUndoingCancelOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        invalidAction(orderId, "undocancel");
    }

    @Test
    void givenAnAccountedOrder_whenUndoingCancelOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        invalidAction(orderId, "undocancel");
    }

    @Test
    void givenAnOpenOrder_whenUndoingCancelOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        invalidAction(orderId, "undocancel");
    }
}
