package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.persistence.entity.AuditUserBalance;
import eu.aequos.gogas.persistence.repository.AuditUserBalanceRepo;
import eu.aequos.gogas.persistence.repository.YearRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.aequos.gogas.persistence.entity.AuditUserBalance.EntryType.ORDER;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderAccountingIntegrationTest extends OrderManagementBaseIntegrationTest {

    @MockBean
    private YearRepo yearRepo;

    @Autowired
    private AuditUserBalanceRepo auditUserBalanceRepo;

    @AfterEach
    void clearUserBalance() {
        mockAccountingData.resetUserBalances();
    }

    @Test
    void givenAClosedOrderComputedAmountEmpty_whenAccountingOrder_thenOrderIsAccounted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        closeOrder(orderId);

        performAction(orderId, "contabilizza");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 2, "Contabilizzato", "dettaglio,storna", false, true);
    }

    @Test
    void givenAClosedOrderComputedAmountWithUsersItems_whenAccountingOrder_thenOrderIsAccountedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -14.1);
        checkBalance(userId2, -27.0);
        checkBalance(userId3, -13.4);

        //no charge for other users
        checkEmptyBalance(orderManagerId1);
        checkEmptyBalance(orderManagerId2);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);
        checkEmptyBalance(mockUsersData.getSimpleUserId());
        checkEmptyBalance(mockUsersData.getDefaultAdminId());

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -14.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -27.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.4, 0.0);
    }

    @Test
    void givenAClosedOrderComputedAmountWithModifiedUsersItems_whenAccountingOrder_thenOrderIsAccountedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 7.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 0.0);
        cancelUserOrder(orderId, userId1, "BIRRA1");
        cancelProductOrder(orderId, "MELE1");

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -5.8);
        checkBalance(userId2, -17.45);
        checkBalance(userId3, 0.0);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -5.8, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -17.45, 0.0);
    }

    @Test
    void givenAClosedOrderComputedAmountWithUsersItemsAndShippingCosts_whenAccountingOrder_thenOrderIsAccountedForUsersWithShippingCosts() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -16.69);
        checkBalance(userId2, -31.95);
        checkBalance(userId3, -15.86);

        //no charge for other users
        checkEmptyBalance(orderManagerId1);
        checkEmptyBalance(orderManagerId2);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);
        checkEmptyBalance(mockUsersData.getSimpleUserId());
        checkEmptyBalance(mockUsersData.getDefaultAdminId());

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -16.69, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -31.95, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -15.86, 0.0);
    }

    @Test
    void givenAClosedOrderComputedAmountAndNoSummaryWithFriendsItems_whenAccountingOrder_thenOrderIsAccountedForUsersAndFriends() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -35.71, Map.of("", -14.88, "friendId1a friendId1a", -14.1, "friendId1b friendId1b", -6.73));
        checkBalance(userId2, -45.1, Map.of("", -27.0, "friendId2 friendId2", -18.1));
        checkBalance(userId3, -13.4);
        checkBalance(friendId1a, -14.1);
        checkBalance(friendId1b, -6.73);
        checkBalance(friendId2, -18.1);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -35.71, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -45.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.4, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -14.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -6.73, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -18.1, 0.0);
    }

    @Test
    void givenAClosedOrderComputedAmountAndNoSummaryWithFriendsItemsAndShippingCost_whenAccountingOrder_thenOrderIsAccountedForUsersAndFriendsWithShippingCosts() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -39.50, Map.of("", -16.46, "friendId1a friendId1a", -15.60, "friendId1b friendId1b", -7.44));
        checkBalance(userId2, -49.89, Map.of("", -29.87, "friendId2 friendId2", -20.02));
        checkBalance(userId3, -14.82);
        checkBalance(friendId1a, -15.60);
        checkBalance(friendId1b, -7.44);
        checkBalance(friendId2, -20.02);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -39.50, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -49.89, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -14.82, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -15.60, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -7.44, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -20.02, 0.0);
    }

    @Test
    void givenAClosedOrderComputedAmountAndNoSummaryWithFriendsItems_whenAccountingOrder_thenOrderIsAccountedOnlyForUsers() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -35.7);
        checkBalance(userId2, -45.1);
        checkBalance(userId3, -13.4);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -35.7, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -45.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.4, 0.0);

        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);
    }

    @Test
    void givenAClosedOrderComputedAmountAndSummaryWithFriendsItemsWithShippingCost_whenAccountingOrder_thenOrderIsAccountedOnlyForUsersWithShippingCost() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -39.49);
        checkBalance(userId2, -49.89);
        checkBalance(userId3, -14.82);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -39.49, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -49.89, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -14.82, 0.0);

        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);
    }

    @Test
    void givenAClosedOrderNotComputedAmountEmpty_whenAccountingOrder_thenOrderIsAccounted() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        closeOrder(orderId);

        performAction(orderId, "contabilizza");

        verifyOrderStatusAndActions(orderId, orderTypeNotComputed.getId(), 2, "Contabilizzato", "dettaglio,storna", false, true);
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithUserItemsButNoAccountingEntries_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/contabilizza", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Il numero di movimenti inseriti è minore degli utenti ordinanti")));
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithUserItemsButLessAccountingEntries_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/contabilizza", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Il numero di movimenti inseriti è minore degli utenti ordinanti")));
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithUserItemsAndAccountingEntriesForEachUser_whenAccountingOrder_thenOrderIsAccountedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -10.0);
        checkBalance(userId2, -15.0);
        checkBalance(userId3, -12.5);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithUserItemsAndShippingCost_whenAccountingOrder_thenOrderIsAccountedForUsersWithShippingCosts() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -12.67);
        checkBalance(userId2, -19.0);
        checkBalance(userId3, -15.83);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -12.67, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -19.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -15.83, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithModifiedUserItems_whenAccountingOrder_thenOrderIsAccountedRegardlessQuantityChanges() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 0.0);

        changeUserOrderItemQuantityNotComputed(orderId, "FETTINE", userId1, 0.0);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -10.0);
        checkBalance(userId2, -15.0);
        checkBalance(userId3, 0.0);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, 0.0, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountWithMoreUserCostsThanUserItems_whenAccountingOrder_thenOrderIsAccountedToAllUsersWithCost() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);
        setUserCost(orderId, orderManagerId1, 25.0);

        changeUserOrderItemQuantityNotComputed(orderId, "FETTINE", userId1, 0.0);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -10.0);
        checkBalance(userId2, -15.0);
        checkBalance(userId3, -12.5);
        checkBalance(orderManagerId1, -25.0);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(4, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(orderManagerId1.toUpperCase()).get(0), AuditUserBalance.OperationType.ADD, -25.0, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountNoSummaryWithFriendItems_whenAccountingOrder_thenOrderIsAccountedForUsersAndFriends() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);
        setUserCost(orderId, friendId1a, 22.87);
        setUserCost(orderId, friendId1b, 5.59);
        setUserCost(orderId, friendId2, 21.38);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -38.46, Map.of("", -10.0, "friendId1a friendId1a", -22.87, "friendId1b friendId1b", -5.59));
        checkBalance(userId2, -36.38, Map.of("", -15.0, "friendId2 friendId2", -21.38));
        checkBalance(userId3, -12.5);
        checkBalance(friendId1a, -22.87);
        checkBalance(friendId1b, -5.59);
        checkBalance(friendId2, -21.38);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -38.46, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -36.38, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -22.87, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -5.59, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -21.38, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountNoSummaryWithFriendItemsWithShippingCost_whenAccountingOrder_thenOrderIsAccountedForUsersAndFriendsWithShippingCost() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);
        setUserCost(orderId, friendId1a, 22.87);
        setUserCost(orderId, friendId1b, 5.59);
        setUserCost(orderId, friendId2, 21.38);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -42.86, Map.of("", -11.14, "friendId1a friendId1a", -25.49, "friendId1b friendId1b", -6.23));
        checkBalance(userId2, -40.55, Map.of("", -16.72, "friendId2 friendId2", -23.83));
        checkBalance(userId3, -13.93);
        checkBalance(friendId1a, -25.49);
        checkBalance(friendId1b, -6.23);
        checkBalance(friendId2, -23.83);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -42.86, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -40.55, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.93, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -25.49, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -6.23, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -23.83, 0.0);
    }

    @Test
    void givenAClosedOrderNotComputedAmountSummaryWithFriendItems_whenAccountingOrder_thenOrderIsAccountedForUsersOnly() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeNotComputed, true);

        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -10.0);
        checkBalance(userId2, -15.0);
        checkBalance(userId3, -12.5);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        mockOrdersData.forceSummaryRequired(orderTypeNotComputed, false);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
    }
    @Test
    void givenAClosedOrderNotComputedAmountSummaryWithFriendItemsWithShippingCost_whenAccountingOrder_thenOrderIsAccountedForUsersOnlyWithShippingCost() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeNotComputed, true);

        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        Map<String, OrderByUserDTO> userTotals = mockMvcGoGas.postDTOList("/api/order/manage/" + orderId + "/shippingCost", BigDecimal.valueOf(10.0), OrderByUserDTO.class).stream()
                .collect(Collectors.toMap(OrderByUserDTO::getUserId, Function.identity()));

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -12.67);
        checkBalance(userId2, -19.0);
        checkBalance(userId3, -15.83);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -12.67, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -19.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -15.83, 0.0);

        mockOrdersData.forceSummaryRequired(orderTypeNotComputed, false);
    }


    @Test
    void givenAClosedOrderExternalWithNoUserItems_whenAccountingOrder_thenOrderIsAccountedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeExternal.getId());

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        performAction(orderId, "contabilizza");

        checkBalance(userId1, -10.0);
        checkBalance(userId2, -15.0);
        checkBalance(userId3, -12.5);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(3, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
    }

    @Test
    void givenANotExistingOrder_whenAccountingOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/action/contabilizza", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/contabilizza", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/contabilizza", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnotherManagerOfOtherOrderType_whenAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");

        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/contabilizza", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledOrder_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "cancel");
        invalidAction(orderId, "contabilizza");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 3);
    }

    @Test
    void givenAnOpenOrder_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        invalidAction(orderId, "contabilizza");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 0);
    }

    @Test
    void givenAnAccountedOrder_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        invalidAction(orderId, "contabilizza");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 2);
    }

    @Test
    void givenAnAccountedOrderComputedAmount_whenUndoingAccountingOrder_thenOrderIsClosed() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");
        performAction(orderId, "tornachiuso");

        verifyOrderStatusAndActions(orderId, orderTypeComputed.getId(), 1, "Chiuso", "gestisci,riapri,contabilizza", true, false);
    }

    @Test
    void givenAnAccountedOrderComputedAmountWithUsersItems_whenUndoingAccountingOrder_thenOrderIsNoMoreChargedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");
        performAction(orderId, "tornachiuso");

        checkEmptyBalance(userId1);
        checkEmptyBalance(userId2);
        checkEmptyBalance(userId3);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -14.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -27.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.4, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(1), AuditUserBalance.OperationType.REMOVE, 14.1, -14.1);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(1), AuditUserBalance.OperationType.REMOVE, 27.0, -27.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(1), AuditUserBalance.OperationType.REMOVE, 13.4, -13.4);
    }

    @Test
    void givenAnAccountedOrderComputedAmountWithFriendsItems_whenUndoingAccountingOrder_thenOrderIsNoMoreChargedForUsersAndFriends() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        performAction(orderId, "contabilizza");
        performAction(orderId, "tornachiuso");

        checkEmptyBalance(userId1);
        checkEmptyBalance(userId2);
        checkEmptyBalance(userId3);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(12, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -35.71, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -45.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -13.4, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -14.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -6.73, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -18.1, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(1), AuditUserBalance.OperationType.REMOVE, 35.71, -35.71);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(1), AuditUserBalance.OperationType.REMOVE, 45.1, -45.1);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(1), AuditUserBalance.OperationType.REMOVE, 13.4, -13.4);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(1), AuditUserBalance.OperationType.REMOVE, 14.1, -14.1);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(1), AuditUserBalance.OperationType.REMOVE, 6.73, -6.73);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(1), AuditUserBalance.OperationType.REMOVE, 18.1, -18.1);
    }

    @Test
    void givenAnAccountedOrderNotComputedAmountWithUserItemsAndAccountingEntriesForEachUser_whenUndoAccountingOrder_thenOrderIsNoMoreChargedForUsers() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);

        performAction(orderId, "contabilizza");
        performAction(orderId, "tornachiuso");

        checkEmptyBalance(userId1);
        checkEmptyBalance(userId2);
        checkEmptyBalance(userId3);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(6, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -10.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -15.0, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(1), AuditUserBalance.OperationType.REMOVE, 10.0, -10.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(1), AuditUserBalance.OperationType.REMOVE, 15.0, -15.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(1), AuditUserBalance.OperationType.REMOVE, 12.5, -12.5);
    }

    @Test
    void givenAnAccountedOrderNotComputedAmountNoSummaryWithFriendItems_whenUndoingAccountingOrder_thenOrderIsNoMoreChargedForUsersAndFriends() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        addNotComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        closeOrder(orderId);

        setUserCost(orderId, userId1, 10.0);
        setUserCost(orderId, userId2, 15.0);
        setUserCost(orderId, userId3, 12.5);
        setUserCost(orderId, friendId1a, 22.87);
        setUserCost(orderId, friendId1b, 5.59);
        setUserCost(orderId, friendId2, 21.38);

        performAction(orderId, "contabilizza");
        performAction(orderId, "tornachiuso");

        checkEmptyBalance(userId1);
        checkEmptyBalance(userId2);
        checkEmptyBalance(userId3);
        checkEmptyBalance(friendId1a);
        checkEmptyBalance(friendId1b);
        checkEmptyBalance(friendId2);

        List<AuditUserBalance> auditEntries = auditUserBalanceRepo.findAllByOrderByTs();
        assertEquals(12, auditEntries.size());

        Map<String, List<AuditUserBalance>> auditEntriesByUserId = auditEntries.stream()
                .collect(Collectors.groupingBy(AuditUserBalance::getUserId));

        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(0), AuditUserBalance.OperationType.ADD, -38.46, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(0), AuditUserBalance.OperationType.ADD, -36.38, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(0), AuditUserBalance.OperationType.ADD, -12.5, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(0), AuditUserBalance.OperationType.ADD, -22.87, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(0), AuditUserBalance.OperationType.ADD, -5.59, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(0), AuditUserBalance.OperationType.ADD, -21.38, 0.0);
        checkBalanceAudit(auditEntriesByUserId.get(userId1).get(1), AuditUserBalance.OperationType.REMOVE, 38.46, -38.46);
        checkBalanceAudit(auditEntriesByUserId.get(userId2).get(1), AuditUserBalance.OperationType.REMOVE, 36.38, -36.38);
        checkBalanceAudit(auditEntriesByUserId.get(userId3).get(1), AuditUserBalance.OperationType.REMOVE, 12.5, -12.5);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1a).get(1), AuditUserBalance.OperationType.REMOVE, 22.87, -22.87);
        checkBalanceAudit(auditEntriesByUserId.get(friendId1b).get(1), AuditUserBalance.OperationType.REMOVE, 5.59, -5.59);
        checkBalanceAudit(auditEntriesByUserId.get(friendId2).get(1), AuditUserBalance.OperationType.REMOVE, 21.38, -21.38);
    }

    @Test
    void givenANotExistingOrder_whenUndoingAccountingOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/action/tornachiuso", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenUndoingAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/tornachiuso", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenUndoingAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/tornachiuso", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnotherManagerOfOtherOrderType_whenUndoingAccountingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/tornachiuso", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACancelledOrder_whenUndoingAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "cancel");
        invalidAction(orderId, "tornachiuso");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 3);
    }

    @Test
    void givenAnOpenOrder_whenUndoingAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        invalidAction(orderId, "tornachiuso");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 0);
    }

    @Test
    void givenAClosedOrder_whenUndoingAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        invalidAction(orderId, "tornachiuso");

        verifyOrderStatus(orderId, orderTypeComputed.getId(), 1);
    }

    @Test
    void givenAClosedYear_whenAccountingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        when(yearRepo.existsYearByYearAndClosed(LocalDate.now().plusDays(7).getYear(), true)).thenReturn(true);

        performAction(orderId, "close");

        invalidAction(orderId, "contabilizza", "L'ordine non può essere contabilizzato, l'anno contabile è chiuso");
    }

    @Test
    void givenAnAccountedOrder_whenAddingInvoiceInformation_thenInformationIsAdded() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/data", invoiceData, BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);
        assertEquals(123.45, orderDetails.getInvoiceAmount().doubleValue(), 0.001);
        assertEquals(LocalDate.now().minusDays(5), orderDetails.getInvoiceDate());
        assertEquals("2022-01", orderDetails.getInvoiceNumber());
        assertEquals(LocalDate.now(), orderDetails.getPaymentDate());
        assertTrue(orderDetails.isPaid());
    }

    @Test
    void givenAnAccountedOrderBilledByAequos_whenAddingInvoiceInformation_thenOnlyPaymentInformationIsAdded() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeAequos.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/data", invoiceData, BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);
        assertNull(orderDetails.getInvoiceAmount());
        assertNull(orderDetails.getInvoiceDate());
        assertNull(orderDetails.getInvoiceNumber());
        assertEquals(LocalDate.now(), orderDetails.getPaymentDate());
        assertTrue(orderDetails.isPaid());
    }

    @Test
    void givenASimpleUserLogin_whenAddingInvoiceInformation_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        mockMvcGoGas.loginAs("user1", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/invoice/data", invoiceData)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAddingInvoiceInformation_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        mockMvcGoGas.loginAs("manager", "password");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/invoice/data", invoiceData)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnInvalidOrderType_whenAddingInvoiceInformation_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        mockMvcGoGas.loginAs("manager", "password");
        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/invoice/data", invoiceData)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANegativeInvoiceAmount_whenAddingInvoiceInformation_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        OrderInvoiceDataDTO invoiceData = new OrderInvoiceDataDTO();
        invoiceData.setInvoiceAmount(BigDecimal.valueOf(-123.45));
        invoiceData.setInvoiceDate(LocalDate.now().minusDays(5));
        invoiceData.setInvoiceNumber("2022-01");
        invoiceData.setPaymentDate(LocalDate.now());
        invoiceData.setPaid(true);

        mockMvcGoGas.post("/api/order/manage/" + orderId + "/invoice/data", invoiceData)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("L'importo fattura deve essere un valore maggiore di zero")));
    }

    @Test
    void givenAnInvoiceDocument_whenAttachingInvoiceDocument_thenFileIsStoredInTheRightFolder() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        File invoice = repoFolder.resolve("integration-test")
                .resolve("invoice")
                .resolve(orderId)
                .toFile();

        assertTrue(invoice.exists());
        assertTrue(invoice.length() > 0);
    }

    @Test
    void givenAnInvalidOrderId_whenAttachingInvoiceDocument_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenAttachingInvoiceDocument_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.loginAs("user1", "password");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenAttachingInvoiceDocument_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.loginAs("manager", "password");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnInvoiceDocumentAttached_whenDownloadingInvoiceDocument_thenAttachmentIsRetrievedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        byte[] contentAsByteArray = mockMvcGoGas.get("/api/order/manage/" + orderId + "/invoice/attachment")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(contentAsByteArray.length > 0);
    }

    @Test
    void givenAContentTypeImage_whenDownloadingInvoiceDocument_thenAttachmentIsRetrievedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.png", "image/png", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        byte[] contentAsByteArray = mockMvcGoGas.get("/api/order/manage/" + orderId + "/invoice/attachment")
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(contentAsByteArray.length > 0);
    }

    @Test
    void givenNoInvoiceDocumentAttached_whenDownloadingInvoiceDocument_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        mockMvcGoGas.get("/api/order/manage/" + orderId + "/invoice/attachment")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnInvalidOrderId_whenDownloadingInvoiceDocument_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/order/manage/" + UUID.randomUUID() + "/invoice/attachment")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenDownloadingInvoiceDocument_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderId + "/invoice/attachment")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenDownloadingInvoiceDocument_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());
        String orderId = createOrder(orderDTO);

        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        performAction(orderId, "close");
        performAction(orderId, "contabilizza");

        InputStream invoiceInputStream = getClass().getResourceAsStream("attachment.pdf");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/invoice/attachment", invoiceInputStream, "invoice.pdf", "application/pdf", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/order/manage/" + orderId + "/invoice/attachment")
                .andExpect(status().isForbidden());
    }

    private void closeOrder(String orderId) throws Exception {
        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));
        performAction(orderId, "close");
    }

    private void addComputedUserOrdersNoFriends(String orderId) throws Exception {
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

    private void addComputedUserOrdersWithFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.5, "KG");
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

    private void addNotComputedUserOrdersNoFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addNotComputedUserOrder(orderId, userId1, "COSTINE", 0.7, "KG");
        addNotComputedUserOrder(orderId, userId1, "FEGATO", 0.8, "KG");
        addNotComputedUserOrder(orderId, userId1, "FETTINE", 0.5, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        addNotComputedUserOrder(orderId, userId2, "FILETTO", 0.5, "KG");
        addNotComputedUserOrder(orderId, userId2, "FEGATO", 0.4, "KG");
        addNotComputedUserOrder(orderId, userId2, "COPPA", 0.35, "KG");

        mockMvcGoGas.loginAs("user3", "password");
        addNotComputedUserOrder(orderId, userId3, "COSTINE", 1.0, "KG");
        addNotComputedUserOrder(orderId, userId3, "FILETTO", 0.5, "KG");
        addNotComputedUserOrder(orderId, userId3, "FEGATO", 0.6, "KG");
        addNotComputedUserOrder(orderId, userId3, "FETTINE", 0.4, "KG");
        addNotComputedUserOrder(orderId, userId3, "COPPA",  0.25, "KG");
    }

    private void addNotComputedUserOrdersWithFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addNotComputedUserOrder(orderId, userId1, "COSTINE", 0.7, "KG");
        addNotComputedUserOrder(orderId, userId1, "FEGATO", 0.8, "KG");
        addNotComputedUserOrder(orderId, userId1, "FETTINE", 0.5, "KG");
        addNotComputedUserOrder(orderId, friendId1a, "COSTINE", 0.9, "KG");
        addNotComputedUserOrder(orderId, friendId1a, "COPPA", 0.3, "KG");
        addNotComputedUserOrder(orderId, friendId1b, "FETTINE", 1.0, "KG");
        addNotComputedUserOrder(orderId, friendId1b, "FEGATO", 0.75, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        addNotComputedUserOrder(orderId, userId2, "FILETTO", 0.5, "KG");
        addNotComputedUserOrder(orderId, userId2, "FEGATO", 0.4, "KG");
        addNotComputedUserOrder(orderId, userId2, "COPPA", 0.35, "KG");
        addNotComputedUserOrder(orderId, friendId2, "COSTINE", 0.4, "KG");
        addNotComputedUserOrder(orderId, friendId2, "FEGATO", 0.65, "KG");

        mockMvcGoGas.loginAs("user3", "password");
        addNotComputedUserOrder(orderId, userId3, "COSTINE", 1.0, "KG");
        addNotComputedUserOrder(orderId, userId3, "FILETTO", 0.5, "KG");
        addNotComputedUserOrder(orderId, userId3, "FEGATO", 0.6, "KG");
        addNotComputedUserOrder(orderId, userId3, "FETTINE", 0.4, "KG");
        addNotComputedUserOrder(orderId, userId3, "COPPA",  0.25, "KG");
    }

    private void checkEmptyBalance(String userId) throws Exception {
        checkBalance(userId, 0.0, Collections.emptyMap());
    }

    private void checkBalance(String userId, double expectedBalance) throws Exception {
        Map<String, Double> expectedCharges = Map.of("", expectedBalance);
        checkBalance(userId, expectedBalance, expectedCharges);
    }

    private void checkBalance(String userId, double expectedBalance, Map<String, Double> expectedCharges) throws Exception {
        BigDecimal balance = mockMvcGoGas.getDTO("/api/accounting/user/" + userId + "/balance", BigDecimal.class);
        assertEquals(expectedBalance, balance.doubleValue(), 0.001);

        UserBalanceSummaryDTO balanceSummary = mockMvcGoGas.getDTO("/api/accounting/user/balance/" + userId, UserBalanceSummaryDTO.class);
        assertEquals(expectedBalance, balanceSummary.getBalance().doubleValue(), 0.001);

        assertEquals(expectedCharges.size(), balanceSummary.getEntries().size());

        Pattern regex = Pattern.compile("^.+\\((.+)\\)$");

        Map<String, BigDecimal> entriesMap = balanceSummary.getEntries().stream().collect(Collectors.toMap(e -> {
            Matcher matcher = regex.matcher(e.getDescription());
            if (matcher.matches()) {
                return matcher.group(1);
            } else {
                return "";
            }
        }, UserBalanceEntryDTO::getAmount));

        expectedCharges.forEach((userFullName, amount) -> assertEquals(amount, entriesMap.get(userFullName).doubleValue()));
    }

    private void checkBalanceAudit(AuditUserBalance auditUserBalance, AuditUserBalance.OperationType operationType,
                                   double amount, double currentBalance) {

        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(currentBalance).setScale(5, RoundingMode.HALF_UP);
        BigDecimal expectedAmount = BigDecimal.valueOf(amount).setScale(5, RoundingMode.HALF_UP);

        assertNotNull(auditUserBalance.getTs());
        assertNotNull(auditUserBalance.getReferenceId());
        assertEquals(ORDER, auditUserBalance.getEntryType());
        assertEquals(operationType, auditUserBalance.getOperationType());
        assertEquals(expectedAmount, auditUserBalance.getAmount());
        assertEquals(expectedCurrentBalance, auditUserBalance.getCurrentBalance());
    }
}
