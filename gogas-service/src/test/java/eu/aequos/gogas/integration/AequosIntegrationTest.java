package eu.aequos.gogas.integration;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.AequosOrderType;
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
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AequosIntegrationTest {

    @Autowired
    private MockMvcGoGas mockMvcGoGas;

    @Autowired
    private MockOrders mockOrders;

    @Autowired
    private MockUsers mockUsers;

    @MockBean
    private AequosApiClient aequosApiClient;

    @BeforeEach
    void setUp() {
        when(aequosApiClient.orderTypes()).thenReturn(Arrays.asList(
                buildOrderType(0, "Fresco Settimanale", true),
                buildOrderType(1, "Pane", true),
                buildOrderType(2, "Carni bianche", false)
        ));
    }

    @AfterEach
    void tearDown() {
        mockOrders.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenRequestingAequosOrderTypeSynch_thenUnauthorizedIsReturned() throws Exception {
        mockUsers.createSimpleUser("simple_user", "simple_user");
        mockMvcGoGas.loginAs("simple_user", "simple_user");

        mockMvcGoGas.put("/api/ordertype/aequos/sync")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenEmptyListOfAequosOrderType_whenRequestingAequosOrderTypeSynch_thenNoOrderTypeIsCreated() throws Exception {
        when(aequosApiClient.orderTypes()).thenReturn(Collections.emptyList());
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        List<OrderType> createdOrderTypes = mockMvcGoGas.getDTOList("/api/ordertype/list", OrderType.class);
        assertEquals(Collections.emptyList(), createdOrderTypes);
    }

    @Test
    void givenAListOfNewAequosOrderType_whenRequestingAequosOrderTypeSynch_thenAllOrderTypeIsCreated() throws Exception {
        mockOrders.createExistingOrderType("Fresco Settimanale", 0);
        mockOrders.createExistingOrderType("Pane", 1);
        mockOrders.createExistingOrderType("Carni Bianche", 2);

        mockMvcGoGas.loginAsAdmin();

        Map<Integer, OrderTypeDTO> existingOrderTypes = getOrderTypesByAequosId();;

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> updatedOrderTypes = getOrderTypesByAequosId();

        assertEquals(updatedOrderTypes, existingOrderTypes);
    }

    @Test
    void givenAListOfExistingAequosOrderType_whenRequestingAequosOrderTypeSynch_thenNoChangesArePerformed() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> createdOrderTypes = getOrderTypesByAequosId();
        assertEquals(3, createdOrderTypes.size());

        checkCreatedAequosOrderType(createdOrderTypes.get(0), "Fresco Settimanale", true);
        checkCreatedAequosOrderType(createdOrderTypes.get(1), "Pane", true);
        checkCreatedAequosOrderType(createdOrderTypes.get(2), "Carni bianche", false);
    }

    private void checkCreatedAequosOrderType(OrderTypeDTO orderType, String description, boolean billedByAequos) {
        assertEquals(description, orderType.getDescription());
        assertEquals(billedByAequos, orderType.isBilledByAequos());

        assertFalse(orderType.isExternal());
        assertTrue(orderType.isComputedAmount());
        assertTrue(orderType.isShowAdvance());
        assertTrue(orderType.isSummaryRequired());
        assertFalse(orderType.isShowBoxCompletion());
        assertFalse(orderType.isExcelAllProducts());
        assertFalse(orderType.isExcelAllUsers());
        assertFalse(orderType.isHasTurns());
        assertFalse(orderType.isUsed());
        assertNull(orderType.getExternalLink());
    }

    private Map<Integer, OrderTypeDTO> getOrderTypesByAequosId() throws Exception {
        return mockMvcGoGas.getDTOList("/api/ordertype/list", OrderTypeDTO.class).stream()
                .collect(Collectors.toMap(OrderTypeDTO::getAequosOrderId, Function.identity()));
    }

    private AequosOrderType buildOrderType(int id, String name, boolean billedByAequos) {
        return AequosOrderType.builder()
                .id(id)
                .description(name)
                .billedByAequos(billedByAequos ? 1 : 0)
                .build();
    }
}
