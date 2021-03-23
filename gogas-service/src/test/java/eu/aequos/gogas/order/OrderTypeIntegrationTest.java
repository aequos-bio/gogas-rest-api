package eu.aequos.gogas.order;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.mock.MockOrders;
import eu.aequos.gogas.mock.MockUsers;
import eu.aequos.gogas.mvc.MockMvcGoGas;
import eu.aequos.gogas.persistence.entity.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderTypeIntegrationTest {

    @Autowired
    private MockMvcGoGas mockMvcGoGas;

    @Autowired
    private MockOrders mockOrders;

    @Autowired
    private MockUsers mockUsers;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        mockOrders.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenCreatingOrderType_thenUnauthorizedIsReturned() throws Exception {
        mockUsers.createSimpleUser("simple_user", "simple_user");
        mockMvcGoGas.loginAs("simple_user", "simple_user");

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
        OrderType testOrder = mockOrders.createExistingOrderType("Test Order", null);

        mockUsers.createSimpleUser("simple_user", "simple_user");
        mockMvcGoGas.loginAs("simple_user", "simple_user");

        mockMvcGoGas.delete("/api/ordertype/" + testOrder.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotUsedOrderType_whenDeletingOrderType_thenOrderTypeIsCorrectlyDeleted() throws Exception {
        OrderType testOrder = mockOrders.createExistingOrderType("Test Order", null);
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.deleteDTO("/api/ordertype/" + testOrder.getId(), BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        mockMvcGoGas.get("/api/ordertype/" + testOrder.getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAUsedOrderType_whenDeletingOrderType_thenOrderTypeCannotBeDeleted() throws Exception {
        OrderType testOrderType = mockOrders.createExistingOrderType("Test Order", null);
        mockOrders.createExistingOrder(testOrderType);
        mockMvcGoGas.loginAsAdmin();

        OrderTypeDTO createdOrderType = mockMvcGoGas.getDTO("/api/ordertype/" + testOrderType.getId(), OrderTypeDTO.class);
        assertTrue(createdOrderType.isUsed());

        mockMvcGoGas.delete("/api/ordertype/" + testOrderType.getId())
                .andExpect(status().isConflict());
    }
}
