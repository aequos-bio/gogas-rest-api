package eu.aequos.gogas.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.integration.api.*;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosSyncOrderIntegrationTest extends BaseGoGasIntegrationTest {

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
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, true, "KG", "Cassa", 1.0, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRSOLE1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, false, "KG", "Cassa", 1.0, 0.2, 3.0, null, "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMECRCR1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "PZ", null, 8.5, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMEOPAL1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "KG", "Cassa", 8.5, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "ORPATGIA1131", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), false, false, true, "KG", "Cassa", 11.0, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "ADDITIONAL", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), false, false, true, "KG", "Cassa", 5.0, 0.8, null, "n", "f")
        ).collect(Collectors.toMap(Product::getExternalId, Function.identity()));
    }

    @BeforeEach
    void setUp() {
        aequosOrder = mockOrdersData.createOrder(aequosOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);
        mockOrdersData.updateExternalOrderId(aequosOrder, "99999");

        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(aequosOrder, productsByExternalId.get(productCode), quantity));

        Map<String, Double> userQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 1.0),
                entry("BIRRSOLE1041", 2.0),
                entry("FRMECRCR1054", 2.5),
                entry("FRMEOPAL1054", 1.5),
                entry("ORPATGIA1131", 3.7),
                entry("ADDITIONAL", 3.7)
        );

        userQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createDeliveredUserOrderItem(aequosOrder.getId(), mockUsersData.getSimpleUserId(), productsByExternalId.get(productCode), quantity));
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteOrder(aequosOrder.getId());
    }

    @Test
    void givenNotExistingOrder_whenSynchronizingAequosOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/aequos/order/synch", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenSynchronizingAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAUserNotOrderManagerLogin_whenSynchronizingAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        User orderManager = mockUsersData.createSimpleUser("manager2", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, otherOrderType);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotAequosOrder_whenSynchronizingAequosOrder_thenErrorIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        Order otherOrder = mockOrdersData.createOrder(otherOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + otherOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Order type is not linked to Aequos")));
    }

    @Test
    void givenAnAequosOrderWithoutExternalId_whenSynchronizingAequosOrder_thenErrorIsReturned() throws Exception {
        Order otherOrder = mockOrdersData.createOrder(aequosOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + otherOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Missing Aequos order id")));
    }

    @Test
    void givenMissingAequosCredentials_whenSynchronizingAequosOrder_thenOrderIsCorrectlySent() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Credenziali per Aequos non trovate o non valide, controllare la configurazione")));
    }

    @Test
    void givenAnErrorReturnedByAequos_whenSynchronizingAequosOrder_thenErrorIsReturned() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setError(true);
        synchResponse.setErrorMessage("Generic error when extracting order");

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Errore durante la sincronizzazione dell'ordine aequos 99999: Generic error when extracting order")));
    }

    @Test
    void givenAValidOrderWithNoVariations_whenSynchronizingAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.2, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.2, 2.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 3.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 1.0);

        mockMvcGoGas.get("/api/order/manage/" + aequosOrder.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datafattura", nullValue()))
                .andExpect(jsonPath("$.numerofattura", nullValue()))
                .andExpect(jsonPath("$.totalefattura", is(120.40)));
    }

    @Test
    void givenAnOrderManager_whenSynchronizingAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.2, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, aequosOrderType);

        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());
    }

    @Test
    void givenAValidOrderWithLessItems_whenSynchronizingAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.2, 0.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 0.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 1.0);
    }

    @Test
    void givenAValidOrderWithNotExistingAdditionalItems_whenSynchronizingAequosOrder_thenAdditionalItemsAreNotCreated() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.2, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0),
                buildSyncItem("NOT_FOUND", 0.5, 1.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.2, 2.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 3.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 1.0);
    }

    @Test
    void givenAValidOrderWithExistingAdditionalItems_whenSynchronizingAequosOrder_thenAdditionalItemsAreCreated() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.2, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0),
                buildSyncItem("ADDITIONAL", 0.8, 12.5)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.2, 2.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 3.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 1.0);
        checkProduct(products,"ADDITIONAL", 0.8, 2.5);
    }

    @Test
    void givenAValidOrderWithBoxesVariations_whenSynchronizingAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 5),
                buildSyncItem("BIRRSOLE1041", 0.2, 4),
                buildSyncItem("FRMECRCR1054", 0.2, 8.5),
                buildSyncItem("FRMEOPAL1054", 0.2, 17.0),
                buildSyncItem("ORPATGIA1131", 0.2, 5.5)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 5);
        checkProduct(products,"BIRRSOLE1041", 0.2, 4);
        checkProduct(products,"FRMECRCR1054", 0.2, 1.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 2.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 0.5);
    }

    @Test
    void givenAValidOrderWithPriceVariations_whenSynchronizingAequosOrder_thenUserOrdersAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.5, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.7, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.9, 11.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.5, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.7, 2.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 3.0);
        checkProduct(products,"ORPATGIA1131", 0.9, 1.0);

        //checking price variation on user side
        mockMvcGoGas.loginAsSimpleUser();

        Map<String, List<String>> requestParams = Map.of("userId", List.of(mockUsersData.getSimpleUserId()));
        Map<String, BigDecimal> userOrders = mockMvcGoGas.getDTOList("/api/order/user/" + aequosOrder.getId() + "/items", UserOrderItemDTO.class, requestParams).stream()
                .collect(Collectors.toMap(UserOrderItemDTO::getProductId, UserOrderItemDTO::getUnitPrice));

        checkUserOrderPrice(userOrders,"BIRRAMBR1041", 0.5);
        checkUserOrderPrice(userOrders,"BIRRSOLE1041", 0.2);
        checkUserOrderPrice(userOrders,"FRMECRCR1054", 0.7);
        checkUserOrderPrice(userOrders,"FRMEOPAL1054", 0.2);
        checkUserOrderPrice(userOrders,"ORPATGIA1131", 0.9);
    }

    @Test
    void givenAValidOrderWithInvoiceInfo_whenSynchronizingAequosOrder_thenInvoiceInfoAreCorrectlyUpdate() throws Exception {
        Map<String, ?> params = Map.ofEntries(
                entry("username", "gas"),
                entry("password", "pwd"),
                entry("order_id", "99999")
        );

        List<OrderSynchItem> orderItems = List.of(
                buildSyncItem("BIRRAMBR1041", 0.2, 10),
                buildSyncItem("BIRRSOLE1041", 0.2, 8),
                buildSyncItem("FRMECRCR1054", 0.2, 17.0),
                buildSyncItem("FRMEOPAL1054", 0.2, 25.5),
                buildSyncItem("ORPATGIA1131", 0.2, 11.0)
        );

        OrderSynchResponse synchResponse = new OrderSynchResponse();
        synchResponse.setOrderTotalAmount(BigDecimal.valueOf(120.40));
        synchResponse.setOrderItems(orderItems);
        synchResponse.setInvoiceNumber("inv_12345");

        when(aequosApiClient.synchOrder(params)).thenReturn(synchResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/synch", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + aequosOrder.getId() + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        checkProduct(products,"BIRRAMBR1041", 0.2, 10);
        checkProduct(products,"BIRRSOLE1041", 0.2, 8);
        checkProduct(products,"FRMECRCR1054", 0.2, 2.0);
        checkProduct(products,"FRMEOPAL1054", 0.2, 3.0);
        checkProduct(products,"ORPATGIA1131", 0.2, 1.0);

        mockMvcGoGas.get("/api/order/manage/" + aequosOrder.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datafattura", is("05/04/2022")))
                .andExpect(jsonPath("$.numerofattura", is("inv_12345")))
                .andExpect(jsonPath("$.totalefattura", is(120.40)));
    }

    private void checkUserOrderPrice(Map<String, BigDecimal> userOrders, String productCode, double expectedPrice) {
        BigDecimal productPrice = userOrders.get(productsByExternalId.get(productCode).getId().toUpperCase());
        assertEquals(expectedPrice, productPrice.doubleValue(), 0.001);
    }

    private void checkProduct(Map<String, OrderByProductDTO> products, String productCode, double expectedPrice, double expectedBoxes) {
        OrderByProductDTO productDTO = products.get(productsByExternalId.get(productCode).getId().toUpperCase());

        assertEquals(expectedBoxes, productDTO.getOrderedBoxes().doubleValue(), 0.001);
        assertEquals(expectedPrice, productDTO.getPrice().doubleValue(), 0.001);
    }

    private OrderSynchItem buildSyncItem(String id, double price, double quantity) {
        OrderSynchItem orderSynchItem = new OrderSynchItem();
        orderSynchItem.setId(id);
        orderSynchItem.setPrice(BigDecimal.valueOf(price));
        orderSynchItem.setQuantity(BigDecimal.valueOf(quantity));
        return orderSynchItem;
    }
}
