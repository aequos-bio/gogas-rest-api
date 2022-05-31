package eu.aequos.gogas.acconting;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class FriendsAccountingIntegrationTest extends BaseGoGasIntegrationTest {

    private String friendId1a;
    private String friendId1b;

    private User friend1a;
    private User friend1b;

    private OrderType orderType;
    private Order order;

    private OrderType externalOrderType;
    private Order externalOrder;

    @BeforeAll
    void createUsersAndReasons() {
        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");
        friend1a = mockUsersData.createFriendUser("friend1a", "password", "friend1a", "friend1a", user1);
        friend1b = mockUsersData.createFriendUser("friend1b", "password", "friend1b", "friend1b", user1);

        friendId1a = friend1a.getId().toUpperCase();
        friendId1b = friend1b.getId().toUpperCase();

        mockAccountingData.createAccountingReason("BON", "Bonifico", "+");
        mockAccountingData.createAccountingReason("QTA", "Quota", "-");
        mockAccountingData.createAccountingReason("ADD", "Addebito", "-");

        orderType = mockOrdersData.createOrderType("test", false);
        order = mockOrdersData.createOrder(orderType, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().minusDays(3),
                Order.OrderStatus.Accounted.getStatusCode(), BigDecimal.ZERO);

        externalOrderType = mockOrdersData.createExternalOrderType("test_external");
        externalOrder = mockOrdersData.createOrder(externalOrderType, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().minusDays(3),
                Order.OrderStatus.Accounted.getStatusCode(), BigDecimal.ZERO, "external-link");
    }

    @AfterEach
    void tearDown() {
        mockAccountingData.deleteAllEntries();
    }

    @Test
    void givenAValidPositiveEntry_whenCreatingFriendAccountingEntry_thenEntryIsCreatedAndUserBalanceIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1a, "BON", 200.0, LocalDate.of(2022, 5, 6));
        BasicResponseDTO response = mockMvcGoGas.postDTO("/api/accounting/friend/entry", accountingEntryDTO, BasicResponseDTO.class);
        assertNotNull(response.getData());

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);
    }

    @Test
    void givenAUserNotReferral_whenCreatingFriendAccountingEntry_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);

        mockMvcGoGas.loginAs("user2", "password");

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1a, "BON", 200.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryDTO)
                .andExpect(status().isForbidden());

        mockMvcGoGas.loginAs("user1", "password");
        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);
    }

    @Test
    void givenAUserNotReferral_whenGettingBalance_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user2", "password");
        mockMvcGoGas.get("/api/accounting/friend/balance/" + friendId1a)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAValidNegativeEntry_whenCreatingFriendAccountingEntry_thenEntryIsCreatedAndUserBalanceIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryUser1 = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        BasicResponseDTO response1 = mockMvcGoGas.postDTO("/api/accounting/friend/entry", accountingEntryUser1, BasicResponseDTO.class);
        assertNotNull(response1.getData());

        AccountingEntryDTO accountingEntryUser2 = buildAccountingEntryDTO(friendId1b, "QTA", 20.75, LocalDate.of(2022, 5, 6));
        BasicResponseDTO response2 = mockMvcGoGas.postDTO("/api/accounting/friend/entry", accountingEntryUser2, BasicResponseDTO.class);
        assertNotNull(response2.getData());


        checkBalance(friendId1a, -10.50);
        checkBalance(friendId1b, -20.75);
    }

    @Test
    void givenAClosedYear_whenCreatingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        mockAccountingData.setYearClosed(2021);

        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2021, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Il movimento non può essere creato, l'anno contabile è chiuso")));
    }

    @Test
    void givenAnEmptyDate_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, null);
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyUserId_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(null, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyReasonCode_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, null, 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyAmount_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", null, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenANegativeAmount_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", -100.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAZeroAmount_whenCreatingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 0.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.post("/api/accounting/friend/entry", accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAChangeInUser_whenModifyingUserAccountingEntry_thenEntryIsModifiedAndBalanceIsChanged() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1b, "BON", 200.0, LocalDate.of(2022, 5, 6));
        BasicResponseDTO response = mockMvcGoGas.putDTO("/api/accounting/friend/entry/" + entryId, accountingEntryDTO, BasicResponseDTO.class);
        assertEquals(entryId, response.getData());

        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 200.0);
    }

    @Test
    void givenAChangeInReasonCode_whenModifyingUserAccountingEntry_thenEntryIsModifiedAndBalanceIsChanged() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1a, "ADD", 200.0, LocalDate.of(2022, 5, 6));
        BasicResponseDTO response = mockMvcGoGas.putDTO("/api/accounting/friend/entry/" + entryId, accountingEntryDTO, BasicResponseDTO.class);
        assertEquals(entryId, response.getData());

        checkBalance(friendId1a, -200.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO modifiedEntry = mockMvcGoGas.getDTO("/api/accounting/friend/entry/" + entryId, AccountingEntryDTO.class);
        assertEquals(entryId, modifiedEntry.getId());
        assertEquals(friendId1a, modifiedEntry.getUserId());
        assertEquals("friend1a friend1a", modifiedEntry.getUserName());
        assertEquals("An entry", modifiedEntry.getDescription());
        assertEquals("ADD", modifiedEntry.getReasonCode());
        assertEquals("-", modifiedEntry.getSign());
        assertEquals(LocalDate.of(2022, 5, 6), modifiedEntry.getDate());
        assertEquals(200.0, modifiedEntry.getAmount().doubleValue(), 0.001);
    }

    @Test
    void givenAChangeInDate_whenModifyingUserAccountingEntry_thenEntryIsModified() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1a, "BON", 200.0, LocalDate.of(2022, 4, 6));
        BasicResponseDTO response = mockMvcGoGas.putDTO("/api/accounting/friend/entry/" + entryId, accountingEntryDTO, BasicResponseDTO.class);
        assertEquals(entryId, response.getData());

        AccountingEntryDTO modifiedEntry = mockMvcGoGas.getDTO("/api/accounting/friend/entry/" + entryId, AccountingEntryDTO.class);
        assertEquals(entryId, modifiedEntry.getId());
        assertEquals(friendId1a, modifiedEntry.getUserId());
        assertEquals("friend1a friend1a", modifiedEntry.getUserName());
        assertEquals("An entry", modifiedEntry.getDescription());
        assertEquals("BON", modifiedEntry.getReasonCode());
        assertEquals("+", modifiedEntry.getSign());
        assertEquals(LocalDate.of(2022, 4, 6), modifiedEntry.getDate());
        assertEquals(200.0, modifiedEntry.getAmount().doubleValue(), 0.001);
    }

    @Test
    void givenAChangeInAmount_whenModifyingUserAccountingEntry_thenEntryIsModified() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(friendId1a, "BON", 50.0, LocalDate.of(2022, 4, 6));
        BasicResponseDTO response = mockMvcGoGas.putDTO("/api/accounting/friend/entry/" + entryId, accountingEntryDTO, BasicResponseDTO.class);
        assertEquals(entryId, response.getData());

        checkBalance(friendId1a, 50.0);
        checkBalance(friendId1b, 0.0);

        AccountingEntryDTO modifiedEntry = mockMvcGoGas.getDTO("/api/accounting/friend/entry/" + entryId, AccountingEntryDTO.class);
        assertEquals(entryId, modifiedEntry.getId());
        assertEquals(friendId1a, modifiedEntry.getUserId());
        assertEquals("friend1a friend1a", modifiedEntry.getUserName());
        assertEquals("An entry", modifiedEntry.getDescription());
        assertEquals("BON", modifiedEntry.getReasonCode());
        assertEquals("+", modifiedEntry.getSign());
        assertEquals(LocalDate.of(2022, 4, 6), modifiedEntry.getDate());
        assertEquals(50.0, modifiedEntry.getAmount().doubleValue(), 0.001);
    }

    @Test
    void givenANotExistingEntry_whenModifyingUserAccountingEntry_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 20.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + UUID.randomUUID(), accountingEntryUser)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAClosedYearOfExistingEntry_whenModifyingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        AccountingEntry entry = mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2021, 5, 6));
        mockAccountingData.setYearClosed(2021);

        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entry.getId(), accountingEntryUser)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Il movimento non può essere modificato, l'anno contabile è chiuso")));
    }

    @Test
    void givenAClosedYearForModifiedEntry_whenModifyingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        AccountingEntry entry = mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2022, 5, 6));
        mockAccountingData.setYearClosed(2021);

        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2021, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entry.getId(), accountingEntryUser)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Il movimento non può essere modificato, l'anno contabile è chiuso")));
    }

    @Test
    void givenAnEmptyDate_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, null);
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyUserId_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(null, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyReasonCode_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, null, 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEmptyAmount_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", null, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenANegativeAmount_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", -100.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAZeroAmount_whenModifyingUserAccountingEntry_thenBadRequestIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 0.0, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenANotFriendReferral_whenModifyingUserAccountingEntry_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        mockMvcGoGas.loginAs("user2", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entryId, accountingEntryUser)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnEntryRelatedToOrder_whenModifyingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        AccountingEntry entry = mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a);

        mockMvcGoGas.loginAs("user1", "password");

        AccountingEntryDTO accountingEntryUser = buildAccountingEntryDTO(friendId1a, "QTA", 10.50, LocalDate.of(2022, 5, 6));
        mockMvcGoGas.put("/api/accounting/friend/entry/" + entry.getId(), accountingEntryUser)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Movimento relativo ad un ordine, non può essere modificato")));
    }

    @Test
    void givenAnExistingPositiveEntry_whenDeletingUserAccountingEntry_thenEntryIsRemovedAndBalanceIsChanged() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        checkBalance(friendId1a, 200.0);
        checkBalance(friendId1b, 0.0);

        BasicResponseDTO response = mockMvcGoGas.deleteDTO("/api/accounting/friend/entry/" + entryId, BasicResponseDTO.class);
        assertEquals("OK", response.getData());

        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);
    }

    @Test
    void givenAnExistingNegativeEntry_whenDeletingUserAccountingEntry_thenEntryIsRemovedAndBalanceIsChanged() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a, "ADD");

        checkBalance(friendId1a, -200.0);
        checkBalance(friendId1b, 0.0);

        BasicResponseDTO response = mockMvcGoGas.deleteDTO("/api/accounting/friend/entry/" + entryId, BasicResponseDTO.class);
        assertEquals("OK", response.getData());

        checkBalance(friendId1a, 0.0);
        checkBalance(friendId1b, 0.0);
    }

    @Test
    void givenAnInvalidEntry_whenDeletingUserAccountingEntry_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/accounting/friend/entry/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotReferralLogin_whenDeletingUserAccountingEntry_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1b, "ADD");

        mockMvcGoGas.loginAs("user2", "password");

        mockMvcGoGas.delete("/api/accounting/friend/entry/" + entryId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnEntryRelatedToOrder_whenDeletingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        AccountingEntry entry = mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/accounting/friend/entry/" + entry.getId())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Movimento relativo ad un ordine, non può essere eliminato")));
    }

    @Test
    void givenAClosedYear_whenDeletingUserAccountingEntry_thenErrorIsReturned() throws Exception {
        AccountingEntry entry = mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2021, 5, 6));
        mockAccountingData.setYearClosed(2021);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/accounting/friend/entry/" + entry.getId())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Il movimento non può essere eliminato, l'anno contabile è chiuso")));
    }

    @Test
    void givenASetOfEntries_whenSearchingByUserId_thenOnlyEntriesOfTheUserAreReturned() throws Exception {
        List<AccountingEntry> entries = List.of(
            mockAccountingData.createAccountingEntry(friend1a, "BON", 200.0, LocalDate.of(2022, 5, 1)),
            mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2022, 5, 2)),
            mockAccountingData.createAccountingEntry(friend1a, "QTA", 50.0, LocalDate.of(2022, 5, 4)),
            mockAccountingData.createAccountingEntry(friend1b, "BON", 250.0, LocalDate.of(2022, 5, 2)),
            mockAccountingData.createAccountingEntry(friend1b, "ADD", 112.34, LocalDate.of(2022, 5, 4)),
            mockAccountingData.createAccountingEntry(friend1b, "QTA", 25.62, LocalDate.of(2022, 5, 7))
        );

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("userId", List.of(friendId1a));
        Map<String, AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams).stream()
                .collect(Collectors.toMap(AccountingEntryDTO::getId, Function.identity()));

        assertEquals(3, searchResults.size());

        verifyEntry(searchResults.get(entries.get(0).getId().toUpperCase()), entries.get(0), friend1a);
        verifyEntry(searchResults.get(entries.get(1).getId().toUpperCase()), entries.get(1), friend1a);
        verifyEntry(searchResults.get(entries.get(2).getId().toUpperCase()), entries.get(2), friend1a);
    }

    @Test
    void givenASetOfEntries_whenSearchingByDates_thenOnlyEntriesOfPeriodAreReturned() throws Exception {
        List<AccountingEntry> entries = List.of(
                mockAccountingData.createAccountingEntry(friend1a, "BON", 200.0, LocalDate.of(2022, 5, 1)),
                mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1a, "QTA", 50.0, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "BON", 250.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1b, "ADD", 112.34, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "QTA", 25.62, LocalDate.of(2022, 5, 7))
        );

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("dateFrom", List.of("04/05/2022"), "dateTo", List.of("07/05/2022"));
        Map<String, AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams).stream()
                .collect(Collectors.toMap(AccountingEntryDTO::getId, Function.identity()));

        assertEquals(3, searchResults.size());

        verifyEntry(searchResults.get(entries.get(2).getId().toUpperCase()), entries.get(2), friend1a);
        verifyEntry(searchResults.get(entries.get(4).getId().toUpperCase()), entries.get(4), friend1b);
        verifyEntry(searchResults.get(entries.get(5).getId().toUpperCase()), entries.get(5), friend1b);
    }

    @Test
    void givenASetOfEntries_whenSearchingByReasonCode_thenOnlyEntriesOfPeriodAreReturned() throws Exception {
        List<AccountingEntry> entries = List.of(
                mockAccountingData.createAccountingEntry(friend1a, "BON", 200.0, LocalDate.of(2022, 5, 1)),
                mockAccountingData.createAccountingEntry(friend1a, "ADD", 100.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1a, "QTA", 50.0, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "BON", 250.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1b, "ADD", 112.34, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "QTA", 25.62, LocalDate.of(2022, 5, 7))
        );

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("reasonCode", List.of("BON"));
        Map<String, AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams).stream()
                .collect(Collectors.toMap(AccountingEntryDTO::getId, Function.identity()));

        assertEquals(2, searchResults.size());

        verifyEntry(searchResults.get(entries.get(0).getId().toUpperCase()), entries.get(0), friend1a);
        verifyEntry(searchResults.get(entries.get(3).getId().toUpperCase()), entries.get(3), friend1b);
    }

    @Test
    void givenASetOfEntries_whenSearchingByDescription_thenOnlyEntriesOfPeriodAreReturned() throws Exception {
        List<AccountingEntry> entries = List.of(
                mockAccountingData.createAccountingEntry(friend1a, "BON", "Charged", 200.0, LocalDate.of(2022, 5, 1)),
                mockAccountingData.createAccountingEntry(friend1a, "ADD", "My entry", 100.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7))
        );

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("description", List.of("entry"));
        Map<String, AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams).stream()
                .collect(Collectors.toMap(AccountingEntryDTO::getId, Function.identity()));

        assertEquals(3, searchResults.size());

        verifyEntry(searchResults.get(entries.get(1).getId().toUpperCase()), entries.get(1), friend1a);
        verifyEntry(searchResults.get(entries.get(4).getId().toUpperCase()), entries.get(4), friend1b);
        verifyEntry(searchResults.get(entries.get(5).getId().toUpperCase()), entries.get(5), friend1b);
    }

    @Test
    void givenASetOfEntries_whenSearchingByMultipleCriteria_thenOnlyEntriesOfPeriodAreReturned() throws Exception {
        List<AccountingEntry> entries = List.of(
                mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1)),
                mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2)),
                mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4)),
                mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7))
        );

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of(
                "userId", List.of(friendId1a),
                "description", List.of("entry"),
                "reasonCode", List.of("BON"),
                "dateFrom", List.of("01/04/2022"),
                "dateTo", List.of("30/05/2022")
        );

        Map<String, AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams).stream()
                .collect(Collectors.toMap(AccountingEntryDTO::getId, Function.identity()));

        assertEquals(1, searchResults.size());

        verifyEntry(searchResults.get(entries.get(0).getId().toUpperCase()), entries.get(0), friend1a);
    }

    @Test
    void givenSearchingCriteriaNotMatchingEntries_whenSearchingByMultipleCriteria_thenNoEntriesAreReturned() throws Exception {
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of(
                "userId", List.of(friendId1a),
                "description", List.of("entry"),
                "reasonCode", List.of("ADD"),
                "dateFrom", List.of("01/04/2022"),
                "dateTo", List.of("02/05/2022")
        );

        List<AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams);
        assertTrue(searchResults.isEmpty());
    }

    @Test
    void givenWrongDates_whenSearchingByDate_thenDatesAreIgnored() throws Exception {
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of(
                "dateFrom", List.of("wrongvalue"),
                "dateTo", List.of("1111111")
        );

        List<AccountingEntryDTO> searchResults = mockMvcGoGas.getDTOList("/api/accounting/friend/entry/list", AccountingEntryDTO.class, requestParams);
        assertEquals(6, searchResults.size());
    }

    @Test
    void givenSomeEntriesForUsers_whenGettingBalanceOfUsers_thenAllInfoAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        Map<String, UserBalanceDTO> userBalanceMap = mockMvcGoGas.getDTOList("/api/accounting/friend/balance", UserBalanceDTO.class).stream()
                .collect(Collectors.toMap(UserBalanceDTO::getUserId, Function.identity()));

        assertEquals(2, userBalanceMap.size());

        UserBalanceDTO balance1 = userBalanceMap.get(friendId1a);
        assertEquals(50.0, balance1.getBalance().doubleValue(), 0.001);
        assertEquals("friend1a friend1a", balance1.getFullName());
        assertEquals("friend1a", balance1.getFirstName());
        assertEquals("friend1a", balance1.getLastName());

        UserBalanceDTO balance2 = userBalanceMap.get(friendId1b);
        assertEquals(112.04, balance2.getBalance().doubleValue(), 0.001);
        assertEquals("friend1b friend1b", balance2.getFullName());
        assertEquals("friend1b", balance2.getFirstName());
        assertEquals("friend1b", balance2.getLastName());
    }

    @Test
    void givenFriendReferralLogin_whenGettingBalanceOfUsers_thenAllInfoAreReturned() throws Exception {
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        mockMvcGoGas.loginAs("user1", "password");

        Map<String, UserBalanceDTO> userBalanceMap = mockMvcGoGas.getDTOList("/api/accounting/friend/balance", UserBalanceDTO.class).stream()
                .collect(Collectors.toMap(UserBalanceDTO::getUserId, Function.identity()));

        assertEquals(2, userBalanceMap.size());
    }

    @Test
    void givenNoFilters_whenGettingBalanceOfSingleUser_thenAllInfoAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntryWithOrderId(externalOrder.getId(), friend1b, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        UserBalanceSummaryDTO userBalance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + friendId1a, UserBalanceSummaryDTO.class);
        assertEquals(-50.0, userBalance.getBalance().doubleValue(), 0.001);
        assertEquals(4, userBalance.getEntries().size());

        verifyEntry(userBalance.getEntries().get(0), LocalDate.of(2022, 5, 4), -50.0, "Quota - another",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(1), LocalDate.of(2022, 5, 2), -100.0, "Addebito - Charged",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(2), LocalDate.of(2022, 5, 1), 200.0, "Bonifico - My entry",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(3), LocalDate.of(2022, 4, 30), -100.0, "Addebito ordine - Addebito ordine",
                null, order.getId().toUpperCase(), orderType.getId().toUpperCase());
    }

    @Test
    void givenReferralLogin_whenGettingBalanceOfSingleUser_thenAllInfoAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntryWithOrderId(externalOrder.getId(), friend1b, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        UserBalanceSummaryDTO userBalance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + friendId1b, UserBalanceSummaryDTO.class);
        assertEquals(12.04, userBalance.getBalance().doubleValue(), 0.001);
        assertEquals(4, userBalance.getEntries().size());

        verifyEntry(userBalance.getEntries().get(0), LocalDate.of(2022, 5, 7), -25.62, "Quota - entry again",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(1), LocalDate.of(2022, 5, 4), -112.34, "Addebito - An entry",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(2), LocalDate.of(2022, 5, 2), 250.0, "Bonifico - transfer",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(3), LocalDate.of(2022, 4, 30), -100.0, "Addebito ordine - Addebito ordine",
                "external-link", externalOrder.getId().toUpperCase(), externalOrderType.getId().toUpperCase());
    }

    @Test
    void givenFilterOnDates_whenGettingBalanceOfSingleUser_thenOnlyEntriesInPeriodAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntryWithOrderId(externalOrder.getId(), friend1b, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        Map<String, List<String>> params = Map.of("dateFrom", List.of("01/05/2022"), "dateTo", List.of("02/05/2022"));
        UserBalanceSummaryDTO userBalance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + friendId1a, UserBalanceSummaryDTO.class, params);
        assertEquals(-50.0, userBalance.getBalance().doubleValue(), 0.001);
        assertEquals(2, userBalance.getEntries().size());

        verifyEntry(userBalance.getEntries().get(0), LocalDate.of(2022, 5, 2), -100.0, "Addebito - Charged",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(1), LocalDate.of(2022, 5, 1), 200.0, "Bonifico - My entry",
                null, null, null);
    }

    @Test
    void givenAPagingParam_whenGettingBalanceOfSingleUser_thenOnlyFirstPageEntriesAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntryWithOrderId(externalOrder.getId(), friend1b, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        Map<String, List<String>> params = Map.of("skipItems", List.of("0"), "maxItems", List.of("2"));
        UserBalanceSummaryDTO userBalance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + friendId1a, UserBalanceSummaryDTO.class, params);
        assertEquals(-50.0, userBalance.getBalance().doubleValue(), 0.001);
        assertEquals(2, userBalance.getEntries().size());

        verifyEntry(userBalance.getEntries().get(0), LocalDate.of(2022, 5, 4), -50.0, "Quota - another",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(1), LocalDate.of(2022, 5, 2), -100.0, "Addebito - Charged",
                null, null, null);
    }

    @Test
    void givenNextPagingParam_whenGettingBalanceOfSingleUser_thenOnlySecondPageEntriesAreReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockAccountingData.createAccountingEntryWithOrderId(order.getId(), friend1a, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1a, "BON", "My entry", 200.0, LocalDate.of(2022, 5, 1));
        mockAccountingData.createAccountingEntry(friend1a, "ADD", "Charged", 100.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1a, "QTA", "another", 50.0, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntryWithOrderId(externalOrder.getId(), friend1b, LocalDate.of(2022, 4, 30));
        mockAccountingData.createAccountingEntry(friend1b, "BON", "transfer", 250.0, LocalDate.of(2022, 5, 2));
        mockAccountingData.createAccountingEntry(friend1b, "ADD", "An entry", 112.34, LocalDate.of(2022, 5, 4));
        mockAccountingData.createAccountingEntry(friend1b, "QTA", "entry again", 25.62, LocalDate.of(2022, 5, 7));

        Map<String, List<String>> params = Map.of("skipItems", List.of("2"), "maxItems", List.of("2"));
        UserBalanceSummaryDTO userBalance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + friendId1a, UserBalanceSummaryDTO.class, params);
        assertEquals(-50.0, userBalance.getBalance().doubleValue(), 0.001);
        assertEquals(2, userBalance.getEntries().size());

        verifyEntry(userBalance.getEntries().get(0), LocalDate.of(2022, 5, 1), 200.0, "Bonifico - My entry",
                null, null, null);

        verifyEntry(userBalance.getEntries().get(1), LocalDate.of(2022, 4, 30), -100.0, "Addebito ordine - Addebito ordine",
                null, order.getId().toUpperCase(), orderType.getId().toUpperCase());
    }

    @Test
    void givenUserANotFriend_whenGettingUserAccountingEntry_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        String entryId = addFirstEntry(friendId1a);

        mockMvcGoGas.loginAs("user2", "password");
        mockMvcGoGas.get("/api/accounting/friend/entry/" + entryId)
                .andExpect(status().isForbidden());
    }

    private void verifyEntry(UserBalanceEntryDTO entry, LocalDate date, double amount,
                           String description, String externalLink, String orderId, String orderTypeId) {

        assertEquals(date, entry.getDate());
        assertEquals(amount, entry.getAmount().doubleValue(), 0.001);
        assertEquals(description, entry.getDescription());
        assertEquals(externalLink, entry.getExternalLink());
        assertEquals(orderId, entry.getOrderId());
        assertEquals(orderTypeId, entry.getOrderType());
    }

    private void verifyEntry(AccountingEntryDTO entry, AccountingEntry expectedEntry, User expectedUser) {
        assertNotNull(entry.getId());
        assertEquals(expectedUser.getId().toUpperCase(), entry.getUserId());
        assertEquals(expectedUser.getFirstName() + " " + expectedUser.getLastName(), entry.getUserName());
        assertEquals(expectedEntry.getDescription(), entry.getDescription());
        assertEquals(expectedEntry.getReason().getReasonCode(), entry.getReasonCode());
        assertEquals(expectedEntry.getReason().getDescription(), entry.getReasonDescription());
        assertEquals(expectedEntry.getReason().getSign(), entry.getSign());
        assertEquals(expectedEntry.getDate(), entry.getDate());
        assertEquals(expectedEntry.getAmount().doubleValue(), entry.getAmount().doubleValue(), 0.001);
    }

    private String addFirstEntry(String userId) throws Exception {
        return addFirstEntry(userId, "BON");
    }

    private String addFirstEntry(String userId, String reasonCode) throws Exception {
        AccountingEntryDTO accountingEntryDTO = buildAccountingEntryDTO(userId, reasonCode, 200.0, LocalDate.of(2022, 5, 6));
        return mockMvcGoGas.postDTO("/api/accounting/friend/entry", accountingEntryDTO, BasicResponseDTO.class).getData().toString();
    }

    private AccountingEntryDTO buildAccountingEntryDTO(String userId, String reasonCode, Double amount, LocalDate date) {
        AccountingEntryDTO accountingEntryUser = new AccountingEntryDTO();
        accountingEntryUser.setUserId(userId);
        accountingEntryUser.setDescription("An entry");
        accountingEntryUser.setReasonCode(reasonCode);
        accountingEntryUser.setAmount(Optional.ofNullable(amount).map(BigDecimal::valueOf).orElse(null));
        accountingEntryUser.setDate(date);
        return accountingEntryUser;
    }

    private void checkBalance(String userId, double expectedBalance) throws Exception {
        UserBalanceSummaryDTO balance = mockMvcGoGas.getDTO("/api/accounting/friend/balance/" + userId, UserBalanceSummaryDTO.class);
        assertEquals(expectedBalance, balance.getBalance().doubleValue(), 0.001);
    }
}
