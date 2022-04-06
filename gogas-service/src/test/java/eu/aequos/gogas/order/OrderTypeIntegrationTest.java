package eu.aequos.gogas.order;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.mock.MockOrdersData;
import eu.aequos.gogas.persistence.entity.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderTypeIntegrationTest extends BaseGoGasIntegrationTest {

    @Autowired
    private MockOrdersData mockOrdersData;

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
}
