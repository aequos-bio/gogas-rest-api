package eu.aequos.gogas.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderDetailsDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.OrderCreatedResponse;
import eu.aequos.gogas.integration.api.OrderCreationItem;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosSendOrderIntegrationTest extends BaseGoGasIntegrationTest {

    @MockBean
    private AequosApiClient aequosApiClient;

    @MockBean
    private ConfigurationRepo configurationRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderType aequosOrderType;
    private Order aequosOrder;
    private Map<String, Product> productsByExternalId;

    @BeforeAll
    void createOrderType() {
        aequosOrderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        Map<String, ProductCategory> expectedCategories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", aequosOrderType.getId())),
                entry("Frutta", mockOrdersData.createCategory("Frutta", aequosOrderType.getId())),
                entry("Ortaggi", mockOrdersData.createCategory("Ortaggi", aequosOrderType.getId()))
        );

        Map<String, Supplier> expectedSuppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)")),
                entry("1131", mockOrdersData.createSupplier("1131", "ABBIATE VALERIO"))
        );

        productsByExternalId = Stream.of(
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRSOLE1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, false, "KG", "Cassa", 6.0, 0.2, 3.0, null, "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMECRCR1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "PZ", null, 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMEOPAL1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "ORPATGIA1131", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f")
        ).collect(Collectors.toMap(Product::getExternalId, Function.identity()));
    }

    @BeforeEach
    void setUp() {
        aequosOrder = mockOrdersData.createOrder(aequosOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteOrder(aequosOrder.getId());
    }

    @Test
    void givenNotExistingOrder_whenSendingAequosOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/aequos/order/send", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenSendingAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAUserNotOrderManagerLogin_whenSendingAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, otherOrderType);

        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotAequosOrder_whenSendingAequosOrder_thenErrorIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        Order otherOrder = mockOrdersData.createOrder(otherOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + otherOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Order type is not linked to Aequos")));
    }

    @Test
    void givenNoProductsToSend_whenSendingAequosOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Order cannot be sent: no products found")));
    }

    @Test
    void givenMissingAequosCredentials_whenSendingAequosOrder_thenOrderIsCorrectlySent() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("99999");
        createdResponse.setTotalItems(24);

        when(aequosApiClient.createOrder(any())).thenReturn(createdResponse);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Credenziali per Aequos non trovate o non valide, controllare la configurazione")));
    }

    @Test
    void givenAValidOrderNotYetSent_whenSendingAequosOrder_thenOrderIsCorrectlySent() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("99999");
        createdResponse.setTotalItems(24);

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, null)))).thenReturn(createdResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        OrderDetailsDTO orderDetailsBefore = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsBefore.isSent());
        assertNull(orderDetailsBefore.getExternalOrderId());

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null, BasicResponseDTO.class);
        assertEquals("99999", responseDTO.getData());

        OrderDetailsDTO orderDetailsAfter = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertTrue(orderDetailsAfter.isSent());
        assertEquals("99999", orderDetailsAfter.getExternalOrderId());
    }

    @Test
    void givenABoxWithZeroQuantity_whenSendingAequosOrder_thenBoxQuantityIsSentAnyway() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 0),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 0),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("99999");
        createdResponse.setTotalItems(12);

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, null)))).thenReturn(createdResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        OrderDetailsDTO orderDetailsBefore = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsBefore.isSent());
        assertNull(orderDetailsBefore.getExternalOrderId());

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null, BasicResponseDTO.class);
        assertEquals("99999", responseDTO.getData());

        OrderDetailsDTO orderDetailsAfter = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertTrue(orderDetailsAfter.isSent());
        assertEquals("99999", orderDetailsAfter.getExternalOrderId());
    }

    @Test
    void givenAnOrderManagerLogin_whenSendingAequosOrder_thenOrderIsSent() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("99999");
        createdResponse.setTotalItems(24);

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, null)))).thenReturn(createdResponse);

        User orderManager = mockUsersData.createSimpleUser("manager2", "password", "manager2", "manager2");
        mockOrdersData.addManager(orderManager, aequosOrderType);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAs("manager2", "password");

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null, BasicResponseDTO.class);
        assertEquals("99999", responseDTO.getData());
    }

    @Test
    void givenANotMatchingNumberOfBoxes_whenSendingAequosOrder_thenErrorIsReturned() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("99999");
        createdResponse.setTotalItems(10);

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, null)))).thenReturn(createdResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        OrderDetailsDTO orderDetailsBefore = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsBefore.isSent());
        assertNull(orderDetailsBefore.getExternalOrderId());

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Errore durante l'invio dell'ordine: i colli inseriti (10) non corrispondono a quelli inviati (24)")));

        OrderDetailsDTO orderDetailsAfter = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsAfter.isSent());
        assertNull(orderDetailsAfter.getExternalOrderId());
    }

    @Test
    void givenAValidOrderAlreadySent_whenSendingAequosOrder_thenOrderIsSentWithId() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 0),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 0),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        //setting previous order id
        mockOrdersData.updateExternalOrderId(aequosOrder, "98765");

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setOrderId("98765");
        createdResponse.setTotalItems(12);

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, "98765")))).thenReturn(createdResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null, BasicResponseDTO.class);
        assertEquals("98765", responseDTO.getData());

        OrderDetailsDTO orderDetailsAfter = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertTrue(orderDetailsAfter.isSent());
        assertEquals("98765", orderDetailsAfter.getExternalOrderId());
    }

    @Test
    void givenAnErrorInAequosResponse_whenSendingAequosOrder_thenErrorIsReturned() throws Exception {
        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        createSupplierOrderItems(boxQuantities);

        OrderCreatedResponse createdResponse = new OrderCreatedResponse();
        createdResponse.setError(true);
        createdResponse.setErrorMessage("Generic error when creating order");

        when(aequosApiClient.createOrder(argThat(params -> hasParams(params, boxQuantities, null)))).thenReturn(createdResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        OrderDetailsDTO orderDetailsBefore = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsBefore.isSent());
        assertNull(orderDetailsBefore.getExternalOrderId());

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/send", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Errore durante l'invio dell'ordine ad Aequos: Generic error when creating order")));

        OrderDetailsDTO orderDetailsAfter = mockMvcGoGas.getDTO("/api/order/manage/" + aequosOrder.getId(), OrderDetailsDTO.class);
        assertFalse(orderDetailsAfter.isSent());
        assertNull(orderDetailsAfter.getExternalOrderId());
    }

    private boolean hasParams(Map<String, ?> params, Map<String, Integer> boxQuantities, String orderId) {
        if (orderId != null && !orderId.equals(params.get("order_id")))
            return false;

        if (!"gas".equals(params.get("username")))
            return false;

        if (!"pwd".equals(params.get("password")))
            return false;

        if (!"0".equals(params.get("tipo_ordine")))
            return false;

        return boxQuantities.equals(parseOrderCreationItems(params.get("rows")));
    }

    private Map<String, Integer> parseOrderCreationItems(Object rows) {
        if (rows == null)
            return null;

        try {
            List<OrderCreationItem> items = objectMapper.readValue(rows.toString(), new TypeReference<List<OrderCreationItem>>() {});

            return items.stream()
                    .collect(Collectors.toMap(OrderCreationItem::getId, item -> item.getBoxesCount().intValue()));

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void createSupplierOrderItems(Map<String, Integer> boxQuantities) {
        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(aequosOrder, productsByExternalId.get(productCode), quantity));
    }
}
