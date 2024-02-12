package eu.aequos.gogas.order;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderTypeIntegrationTest extends BaseGoGasIntegrationTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenCreatingOrderType_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        OrderTypeDTO orderTypeDTO = new OrderTypeDTO();
        orderTypeDTO.setDescription("Test Order");

        mockMvcGoGas.post("/api/ordertype", orderTypeDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANewOrderType_whenCreatingOrderType_thenOrderTypeIsCorrectlyCreated() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        OrderTypeDTO requestOrderTypeDTO = new OrderTypeDTO();
        requestOrderTypeDTO.setDescription("Test Order");
        requestOrderTypeDTO.setComputedAmount(true);
        requestOrderTypeDTO.setShowBoxCompletion(true);
        requestOrderTypeDTO.setShowAdvance(true);
        requestOrderTypeDTO.setSummaryRequired(true);

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/ordertype", requestOrderTypeDTO, BasicResponseDTO.class);
        String id = basicResponseDTO.getData().toString();
        requestOrderTypeDTO.setId(id);

        OrderTypeDTO createdOrderType = mockMvcGoGas.getDTO("/api/ordertype/" + id, OrderTypeDTO.class);
        assertEquals(requestOrderTypeDTO, createdOrderType);
    }

    @Test
    void givenANotUsedOrderTypeAndASimpleUser_whenDeletingOrderType_thenUnauthorizedIsReturned() throws Exception {
        OrderType testOrder = mockOrdersData.createAequosOrderType("Test Order", null);
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.delete("/api/ordertype/" + testOrder.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotUsedOrderType_whenDeletingOrderType_thenOrderTypeIsCorrectlyDeleted() throws Exception {
        OrderType testOrder = mockOrdersData.createAequosOrderType("Test Order", null);
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.deleteDTO("/api/ordertype/" + testOrder.getId(), BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        mockMvcGoGas.get("/api/ordertype/" + testOrder.getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAUsedOrderType_whenDeletingOrderType_thenOrderTypeCannotBeDeleted() throws Exception {
        OrderType testOrderType = mockOrdersData.createAequosOrderType("Test Order", null);
        mockOrdersData.createOpenOrder(testOrderType);
        mockMvcGoGas.loginAsAdmin();

        OrderTypeDTO createdOrderType = mockMvcGoGas.getDTO("/api/ordertype/" + testOrderType.getId(), OrderTypeDTO.class);
        assertTrue(createdOrderType.isUsed());

        mockMvcGoGas.delete("/api/ordertype/" + testOrderType.getId())
                .andExpect(status().isConflict());
    }

    @Test
    void givenAUserWithNoBlacklist_whenAddingUserBlacklist_thenBlacklistIsAdded() throws Exception {
        OrderType orderType1 = mockOrdersData.createOrderType("Order Type 1");
        OrderType orderType2 = mockOrdersData.createOrderType("Order Type 2");

        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");

        String userId1 = user1.getId().toUpperCase();
        String userId2 = user2.getId().toUpperCase();

        mockMvcGoGas.loginAsAdmin();

        verifyBlackList(userId1, Collections.emptyList());
        verifyBlackList(userId2, Collections.emptyList());

        mockMvcGoGas.put("/api/ordertype/blacklist/" + userId1, List.of(orderType1.getId(), orderType2.getId()))
                .andExpect(status().isOk());

        verifyBlackList(userId1, List.of(orderType1.getId().toUpperCase(), orderType2.getId().toUpperCase()));
        verifyBlackList(userId2, Collections.emptyList());
    }

    @Test
    void givenAUserWithBlacklist_whenChangingUserBlacklist_thenBlacklistIsReplaced() throws Exception {
        OrderType orderType1 = mockOrdersData.createOrderType("Order Type 1");
        OrderType orderType2 = mockOrdersData.createOrderType("Order Type 2");

        User user = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        String userId = user.getId().toUpperCase();

        mockOrdersData.addBlacklist(userId, orderType1);

        mockMvcGoGas.loginAsAdmin();

        verifyBlackList(userId, List.of(orderType1.getId().toUpperCase()));

        mockMvcGoGas.put("/api/ordertype/blacklist/" + userId, List.of(orderType2.getId()))
                .andExpect(status().isOk());

        verifyBlackList(userId, List.of(orderType2.getId().toUpperCase()));
    }

    @Test
    void givenAUserWithBlacklist_whenChangingUserBlacklistWithEmpty_thenBlacklistIsRemoved() throws Exception {
        OrderType orderType1 = mockOrdersData.createOrderType("Order Type 1");
        OrderType orderType2 = mockOrdersData.createOrderType("Order Type 2");

        User user = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        String userId = user.getId().toUpperCase();

        mockOrdersData.addBlacklist(userId, orderType1);
        mockOrdersData.addBlacklist(userId, orderType2);

        mockMvcGoGas.loginAsAdmin();

        verifyBlackList(userId, List.of(orderType1.getId().toUpperCase(), orderType2.getId().toUpperCase()));

        mockMvcGoGas.put("/api/ordertype/blacklist/" + userId, Collections.emptyList())
                .andExpect(status().isOk());

        verifyBlackList(userId, Collections.emptyList());
    }

    @Test
    void givenASimpleUser_whenGettingBlacklist_thenUnauthorizedIsReturned() throws Exception {
        User user = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        String userId = user.getId().toUpperCase();

        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.get("/api/ordertype/blacklist/" + userId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenUpdatingBlacklist_thenUnauthorizedIsReturned() throws Exception {
        OrderType orderType = mockOrdersData.createOrderType("Order Type");

        User user = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        String userId = user.getId().toUpperCase();

        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.put("/api/ordertype/blacklist/" + userId, List.of(orderType.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenInvalidUser_whenAddingUserBlacklist_thenErrorIsReturned() throws Exception {
        OrderType orderType = mockOrdersData.createOrderType("Order Type");

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.put("/api/ordertype/blacklist/" + UUID.randomUUID(), List.of(orderType.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenInvalidOrderType_whenAddingUserBlacklist_thenErrorIsReturned() throws Exception {
        User user = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        String userId = user.getId().toUpperCase();

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.put("/api/ordertype/blacklist/" + userId, List.of(UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    private void verifyBlackList(String userId, List<String> expectedBlacklist) throws Exception {
        List<SelectItemDTO> blacklistUser = mockMvcGoGas.getDTOList("/api/ordertype/blacklist/" + userId, SelectItemDTO.class);

        List<String> orderTypeIds = blacklistUser.stream()
                .map(SelectItemDTO::getId)
                .collect(Collectors.toList());

        assertEquals(expectedBlacklist, orderTypeIds);
    }
}
