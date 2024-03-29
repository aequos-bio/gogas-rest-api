package eu.aequos.gogas.integration;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.AequosOrderType;
import eu.aequos.gogas.persistence.entity.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosOrderTypeSyncIntegrationTest extends BaseGoGasIntegrationTest {

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
        mockOrdersData.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenRequestingAequosOrderTypeSynch_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

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
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> createdOrderTypes = getOrderTypesByAequosId();
        assertEquals(3, createdOrderTypes.size());

        checkCreatedAequosOrderType(createdOrderTypes.get(0), "Fresco Settimanale", true, null);
        checkCreatedAequosOrderType(createdOrderTypes.get(1), "Pane", true, null);
        checkCreatedAequosOrderType(createdOrderTypes.get(2), "Carni bianche", false, null);
    }

    @Test
    void givenAListOfExistingAequosOrderType_whenRequestingAequosOrderTypeSynch_thenNoChangesArePerformed() throws Exception {
        mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        mockOrdersData.createAequosOrderType("Pane", 1);
        mockOrdersData.createAequosOrderType("Carni Bianche", 2);

        mockMvcGoGas.loginAsAdmin();

        Map<Integer, OrderTypeDTO> existingOrderTypes = getOrderTypesByAequosId();;

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> updatedOrderTypes = getOrderTypesByAequosId();

        assertEquals(updatedOrderTypes, existingOrderTypes);
    }

    @Test
    void givenAListOfPartiallyExistingAequosOrderType_whenRequestingAequosOrderTypeSynch_thenOnlyNewAreAdded() throws Exception {
        mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        mockOrdersData.createAequosOrderType("Carni Bianche", 2);

        mockMvcGoGas.loginAsAdmin();

        Map<Integer, OrderTypeDTO> existingOrderTypes = getOrderTypesByAequosId();;

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> updatedOrderTypes = getOrderTypesByAequosId();

        assertEquals(updatedOrderTypes.get(0), existingOrderTypes.get(0));
        assertEquals(updatedOrderTypes.get(2), existingOrderTypes.get(2));
        checkCreatedAequosOrderType(updatedOrderTypes.get(1), "Pane", true, null);
    }

    @Test
    void givenAListOfNewAequosOrderTypeAndAequosAccountingCodeAlreadySet_whenRequestingAequosOrderTypeSynch_thenOrderTypesAreCreatedWithAccountingCode() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockOrdersData.createAequosOrderType("An existing order", 5, "F_AEQUOS");

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.putDTO("/api/ordertype/aequos/sync", BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<Integer, OrderTypeDTO> existingOrderTypes = getOrderTypesByAequosId();
        assertEquals(4, existingOrderTypes.size());

        checkCreatedAequosOrderType(existingOrderTypes.get(0), "Fresco Settimanale", true, "F_AEQUOS");
        checkCreatedAequosOrderType(existingOrderTypes.get(1), "Pane", true, "F_AEQUOS");
        checkCreatedAequosOrderType(existingOrderTypes.get(2), "Carni bianche", false, null);
    }

    private void checkCreatedAequosOrderType(OrderTypeDTO orderType, String description,
                                             boolean billedByAequos, String accountingCode) {

        assertEquals(description, orderType.getDescription());
        assertEquals(billedByAequos, orderType.isBilledByAequos());

        assertTrue(orderType.isComputedAmount());
        assertTrue(orderType.isShowAdvance());
        assertFalse(orderType.isSummaryRequired());
        assertFalse(orderType.isShowBoxCompletion());
        assertFalse(orderType.isExcelAllProducts());
        assertFalse(orderType.isExcelAllUsers());
        assertFalse(orderType.isExternal());
        assertFalse(orderType.isHasTurns());
        assertFalse(orderType.isUsed());
        assertNull(orderType.getExternalLink());
        assertEquals(accountingCode, orderType.getAccountingCode());
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
