package eu.aequos.gogas.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.SmallUserOrderItemDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.OrderCreationItem;
import eu.aequos.gogas.integration.api.WeightsUpdatedResponse;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosUpdateWeightsIntegrationTest extends BaseGoGasIntegrationTest {

    @MockBean
    private AequosApiClient aequosApiClient;

    @MockBean
    private ConfigurationRepo configurationRepo;

    @Autowired
    private SupplierOrderItemRepo supplierOrderItemRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderType aequosOrderType;
    private Order aequosOrder;
    private Map<String, Product> productsByExternalId;
    private Map<String, Integer> boxQuantities;
    private Map<String, Double> expectedQuantities;

    private String userId1;
    private String userId2;
    private String userId3;

    @BeforeAll
    void createOrderType() {
        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");
        User user3 = mockUsersData.createSimpleUser("user3", "password", "user3", "user3");

        userId1 = user1.getId().toUpperCase();
        userId2 = user2.getId().toUpperCase();
        userId3 = user3.getId().toUpperCase();

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
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), true, false, false, "PZ", null, 1.0, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "BIRRSOLE1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), true, false, false, "PZ", null, 1.0, 0.2, 3.0, null, "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMECRCR1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "FRMEOPAL1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "ORPATGIA1131", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), true, false, false, "KG", "Cassa", 11.0, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(aequosOrderType.getId(), "ADDITIONAL", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), true, false, false, "KG", "Cassa", 5.0, 0.8, null, "n", "f")
        ).collect(Collectors.toMap(Product::getExternalId, Function.identity()));
    }

    @BeforeEach
    void setUp() throws Exception {
        aequosOrder = mockOrdersData.createOpenOrder(aequosOrderType);

        addUserOrders(aequosOrder.getId());
        closeOrder(aequosOrder.getId());

        mockOrdersData.updateExternalOrderId(aequosOrder, "99999");

        boxQuantities = Map.ofEntries(
                entry("BIRRAMBR1041", 10),
                entry("BIRRSOLE1041", 8),
                entry("FRMECRCR1054", 2),
                entry("FRMEOPAL1054", 3),
                entry("ORPATGIA1131", 1)
        );

        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(aequosOrder, productsByExternalId.get(productCode), quantity));

        expectedQuantities = Map.ofEntries(
                entry("FRMECRCR1054", 15.0),
                entry("ORPATGIA1131", 14.0),
                entry("BIRRAMBR1041", 3.0)
        );
    }

    void addComputedUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        addUserOrder(orderId, userId, productsByExternalId.get(productCode).getId(), qty, um);
    }

    private void addUserOrder(String orderId, String userId, String productId, double qty, String um) throws Exception {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productId);
        request.setQuantity(BigDecimal.valueOf(qty));
        request.setUnitOfMeasure(um);

        mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    private void addUserOrders(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "FRMECRCR1054", 3.0, "KG");
        addComputedUserOrder(orderId, userId1, "ORPATGIA1131", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRAMBR1041", 1.0, "PZ");

        mockMvcGoGas.loginAs("user2", "password");
        addComputedUserOrder(orderId, userId2, "FRMECRCR1054", 1.0, "Cassa");
        addComputedUserOrder(orderId, userId2, "ORPATGIA1131", 4.5, "KG");
        addComputedUserOrder(orderId, userId2, "BIRRAMBR1041", 2.0, "PZ");

        mockMvcGoGas.loginAs("user3", "password");
        addComputedUserOrder(orderId, userId3, "FRMECRCR1054", 3.5, "KG");
        addComputedUserOrder(orderId, userId3, "ORPATGIA1131", 5.5, "KG");
    }

    private void closeOrder(String orderId) throws Exception {
        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));

        mockMvcGoGas.loginAsAdmin();
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/" + "close", null, BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteOrder(aequosOrder.getId());
    }

    @Test
    void givenNotExistingOrder_whenUpdatingWeightsForAequosOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + UUID.randomUUID() + "/aequos/order/weights", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUserLogin_whenUpdatingWeightsForAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAUserNotOrderManagerLogin_whenUpdatingWeightsForAequosOrder_thenUnauthorizedIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        User orderManager = mockUsersData.createSimpleUser("manager2", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, otherOrderType);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotAequosOrder_whenUpdatingWeightsForAequosOrder_thenErrorIsReturned() throws Exception {
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");
        Order otherOrder = mockOrdersData.createOrder(otherOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + otherOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Order type is not linked to Aequos")));
    }

    @Test
    void givenAnAequosOrderWithoutExternalId_whenUpdatingWeightsForAequosOrder_thenErrorIsReturned() throws Exception {
        Order otherOrder = mockOrdersData.createOrder(aequosOrderType, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + otherOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Missing Aequos order id")));
    }

    @Test
    void givenMissingAequosCredentials_whenUpdatingWeightsForAequosOrder_thenOrderIsCorrectlySent() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Credenziali per Aequos non trovate o non valide, controllare la configurazione")));
    }

    @Test
    void givenAnErrorReturnedByAequos_whenUpdatingWeightsForAequosOrder_thenErrorIsReturned() throws Exception {
        WeightsUpdatedResponse updateResponse = new WeightsUpdatedResponse();
        updateResponse.setError(true);
        updateResponse.setErrorMessage("Generic error when extracting order");

        when(aequosApiClient.updateWeight(argThat(params -> hasParams(params, expectedQuantities, "99999")))).thenReturn(updateResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Errore durante l'invio dei pesi per l'ordine aequos 99999: Generic error when extracting order")));
    }

    @Test
    void givenAValidUpdateWeightResponse_whenUpdatingWeightsForAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        WeightsUpdatedResponse updateResponse = new WeightsUpdatedResponse();
        updateResponse.setUpdatedItems(List.of("FRMECRCR1054", "FRMEOPAL1054"));

        when(aequosApiClient.updateWeight(argThat(params -> hasParams(params, expectedQuantities, "99999")))).thenReturn(updateResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null, BasicResponseDTO.class);
        assertEquals(2, responseDTO.getData());

        mockMvcGoGas.get("/api/order/manage/" + aequosOrder.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesiInviati", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        Set<String> updatedSupplierOrderItems = mockOrdersData.getSupplierOrderItems(aequosOrder.getId()).stream()
                .filter(SupplierOrderItem::isWeightUpdated)
                .map(SupplierOrderItem::getProductExternalCode)
                .collect(Collectors.toSet());

        assertEquals(Set.of("FRMECRCR1054", "FRMEOPAL1054"), updatedSupplierOrderItems);
    }

    @Test
    void givenAnOrderManagerLogin_whenUpdatingWeightsForAequosOrder_thenOrderItemsAreCorrectlyUpdate() throws Exception {
        WeightsUpdatedResponse updateResponse = new WeightsUpdatedResponse();
        updateResponse.setUpdatedItems(List.of("FRMECRCR1054", "ORPATGIA1131"));

        when(aequosApiClient.updateWeight(argThat(params -> hasParams(params, expectedQuantities, "99999")))).thenReturn(updateResponse);

        when(configurationRepo.findValueByKey("aequos.username")).thenReturn(Optional.of("gas"));
        when(configurationRepo.findValueByKey("aequos.password")).thenReturn(Optional.of("pwd"));

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, aequosOrderType);

        mockMvcGoGas.loginAs("manager", "password");

        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + aequosOrder.getId() + "/aequos/order/weights", null, BasicResponseDTO.class);
        assertEquals(2, responseDTO.getData());
    }

    private boolean hasParams(Map<String, ?> params, Map<String, Double> boxQuantities, String orderId) {
        if (orderId != null && !orderId.equals(params.get("order_id")))
            return false;

        if (!"gas".equals(params.get("username")))
            return false;

        if (!"pwd".equals(params.get("password")))
            return false;

        if (!"0".equals(params.get("tipo_ordine")))
            return false;

        Map<String, Double> actualRows = parseOrderCreationItems(params.get("rows"));
        return boxQuantities.equals(actualRows);
    }

    private Map<String, Double> parseOrderCreationItems(Object rows) {
        if (rows == null)
            return null;

        try {
            List<OrderCreationItem> items = objectMapper.readValue(rows.toString(), new TypeReference<>() {});

            return items.stream()
                    .collect(Collectors.toMap(OrderCreationItem::getId, item -> item.getBoxesCount().doubleValue()));

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
