package eu.aequos.gogas.order.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderWorkflowIntegrationTest extends BaseGoGasIntegrationTest {

    @MockBean
    private ConfigurationRepo configurationRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderType orderTypeComputed;
    private OrderType orderTypeNotComputed;
    private OrderType orderTypeExternal;
    private OrderType orderTypeAequos;

    private Map<String, Product> productsByCodeComputed;
    private Map<String, Product> productsByCodeNotComputed;

    private String userId1;
    private String userId2;
    private String userId3;
    private String orderManagerId1;
    private String orderManagerId2;

    private List<String> createdOrderIds;

    @BeforeAll
    void createOrderTypeAndUsers() {
        orderTypeComputed = mockOrdersData.createOrderType("Fresco Settimanale", true);

        Map<String, ProductCategory> categories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", orderTypeComputed.getId())),
                entry("Frutta", mockOrdersData.createCategory("Frutta", orderTypeComputed.getId())),
                entry("Ortaggi", mockOrdersData.createCategory("Ortaggi", orderTypeComputed.getId()))
        );

        Map<String, Supplier> suppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)")),
                entry("1131", mockOrdersData.createSupplier("1131", "ABBIATE VALERIO"))
        );

        List<Product> products = List.of(
                mockOrdersData.createProduct(orderTypeComputed.getId(), "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), "PZ", null, 1.0, 3.65),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), "PZ", null, 1.0, 3.65),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "MELE1", "MELE CRIMSON CRISP - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), "KG", "Cassa", 8.5, 1.55),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "MELE2", "MELE OPAL - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), "KG", "Cassa", 8.5, 1.70),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                        suppliers.get("1131"), categories.get("Ortaggi"), "KG", "Cassa", 11.0, 1.45)
        );

        productsByCodeComputed = products.stream().collect(Collectors.toMap(Product::getExternalId, Function.identity()));

        orderTypeNotComputed = mockOrdersData.createOrderType("Cirenaica", false);

        Map<String, ProductCategory> categoriesNotComputed = Map.ofEntries(
                entry("Carne Fresca", mockOrdersData.createCategory("Carne Fresca", orderTypeNotComputed.getId())),
                entry("Bovino", mockOrdersData.createCategory("Bovino", orderTypeNotComputed.getId())),
                entry("Affettati", mockOrdersData.createCategory("Affettati", orderTypeNotComputed.getId()))
        );

        Supplier supplierNotComputed = mockOrdersData.createSupplier("1111", "Cirenaica");


        List<Product> productsNotComputed = List.of(
                mockOrdersData.createProduct(orderTypeComputed.getId(), "COSTINE", "Costine",
                        supplierNotComputed, categoriesNotComputed.get("Carne Fresca"), "KG", null, 1.0, 5.0),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "FILETTO", "Filetto di maiale",
                        supplierNotComputed, categoriesNotComputed.get("Carne Fresca"), "KG", null, 1.0, 4.5),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "FEGATO", "MFegato di Bovino",
                        supplierNotComputed, categoriesNotComputed.get("Bovino"), "KG", null, 1.0, 10.6),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "FETTINE", "Fettine",
                        supplierNotComputed, categoriesNotComputed.get("Bovino"), "KG", null, 1.0, 4.85),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "COPPA", "Coppa stagionata",
                        supplierNotComputed, categoriesNotComputed.get("Affettati"), "PZ", null, 1.0, 8.3)
        );

        productsByCodeNotComputed = productsNotComputed.stream().collect(Collectors.toMap(Product::getExternalId, Function.identity()));

        orderTypeExternal = mockOrdersData.createExternalOrderType("Tomasoni");

        orderTypeAequos = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        userId1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1").getId().toUpperCase();
        userId2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2").getId().toUpperCase();
        userId3 = mockUsersData.createSimpleUser("user3", "password", "user3", "user3").getId().toUpperCase();

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, orderTypeComputed);
        mockOrdersData.addManager(orderManager, orderTypeExternal);
        orderManagerId1 = orderManager.getId();

        User otherManager = mockUsersData.createSimpleUser("manager2", "password", "manager2", "manager2");
        mockOrdersData.addManager(otherManager, orderTypeNotComputed);
        mockOrdersData.addManager(otherManager, orderTypeAequos);
        orderManagerId2 = otherManager.getId();

        createdOrderIds = new ArrayList<>();
    }

    @BeforeEach
    void setUp() {
        /*orderComputed = mockOrdersData.createOrder(orderTypeComputed, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        Map<String, Integer> boxQuantities = Map.ofEntries(
                entry("BIRRA1", 10),
                entry("BIRRA2", 8),
                entry("MELE1", 1),
                entry("MELE2", 3),
                entry("PATATE", 1)
        );

        boxQuantities
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(orderComputed, productsByCodeComputed.get(productCode), quantity));

        Map<String, Map<String, Double>> userQuantities = Map.of(
                userId1,
                Map.ofEntries(
                        entry("BIRRA1", 1.0),
                        entry("MELE1", 2.5),
                        entry("MELE2", 1.5),
                        entry("PATATE", 10.8)
                ),
                userId2,
                Map.ofEntries(
                        entry("BIRRA2", 2.0),
                        entry("MELE1", 2.0),
                        entry("PATATE", 4.7)
                ),
                userId3,
                Map.ofEntries(
                        entry("BIRRA2", 2.0),
                        entry("MELE1", 4.0),
                        entry("MELE2", 1.5)
                )
        );

        createUserOrders(orderComputed, productsByCodeComputed, userQuantities);

        orderNotComputed = mockOrdersData.createOrder(orderTypeNotComputed, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);

        Map<String, Integer> boxQuantitiesNotComputed = Map.ofEntries(
                entry("COSTINE", 1),
                entry("FILETTO", 1),
                entry("FEGATO", 1),
                entry("FETTINE", 1),
                entry("COPPA", 1)
        );

        boxQuantitiesNotComputed
                .forEach((productCode, quantity) -> mockOrdersData.createSupplierOrderItem(orderNotComputed, productsByCodeNotComputed.get(productCode), quantity));

        Map<String, Map<String, Double>> userQuantitiesNotComputed = Map.of(
                userId1,
                Map.ofEntries(
                        entry("COSTINE", 0.7),
                        entry("FEGATO", 0.8),
                        entry("FETTINE", 0.5)
                ),
                userId2,
                Map.ofEntries(
                        entry("FILETTO", 0.5),
                        entry("FEGATO", 0.4),
                        entry("COPPA", 0.35)
                ),
                userId3,
                Map.ofEntries(
                        entry("COSTINE", 1.0),
                        entry("FILETTO", 0.5),
                        entry("FEGATO", 0.6),
                        entry("FETTINE", 0.4),
                        entry("COPPA", 0.25)
                )
        );

        createUserOrders(orderNotComputed, productsByCodeNotComputed, userQuantitiesNotComputed);

        orderExternal = mockOrdersData.createOrder(orderTypeExternal, "2022-03-20", "2022-03-30", "2022-04-05", Order.OrderStatus.Closed);*/
    }

    @AfterEach
    void tearDown() {
        createdOrderIds.forEach(mockOrdersData::deleteOrder);
        createdOrderIds.clear();
    }

    @Test
    void givenAValidOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(),
                 null, false, null, true, false);
    }

    @Test
    void givenAManagerOfOtherOrderType_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUser_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenCreatingOrderWithAdminLogin_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        OrderDTO orderDTO = buildValidOrderDTO(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAValidOrderTypeNotComputed_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeNotComputed.getId().toUpperCase(), orderTypeNotComputed.getDescription(), null, false,
                null, false, false);
    }

    @Test
    void givenAManagerOfOtherOrderTypeComputed_whenCreatingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeNotComputed.getId());

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnExternalOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeExternal.getId(), "aValidUrl");

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeExternal.getId().toUpperCase(), orderTypeExternal.getDescription(), null,
                true, "aValidUrl", false, false);
    }

    @Test
    void givenAnAequosOrderType_whenCreatingOrder_thenOrderIsCreatedCorrectly() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeAequos.getId());

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeAequos.getId().toUpperCase(), orderTypeAequos.getDescription(), 0,
                false, null, false, true);
    }

    @Test
    void givenOpeningDateInTheFuture_whenCreatingOrder_thenOrderIsCreatedCorrectlyAndActionsIncludeDelete() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4), 10, LocalDate.now().plusDays(6), null);

        String orderId = createOrder(orderDTO);

        verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(), null,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), 10, LocalDate.now().plusDays(6), "modifica,elimina",
                false, null, true, false);
    }

    @Test
    void givenOpenedOrderWithoutUserItems_whenDeletingOrder_thenOrderIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        BasicResponseDTO creationResponse = mockMvcGoGas.deleteDTO("/api/order/manage/" + orderId, BasicResponseDTO.class);
        assertEquals("OK", creationResponse.getData());

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeComputed.getId());

        assertTrue(mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .noneMatch(order -> order.getId().equals(orderId.toUpperCase())));

        createdOrderIds.remove(orderId);
    }

    @Test
    void givenOpenedOrderWithUserItems_whenDeletingOrder_thenOrderNotIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        addUserOrder(orderId, orderManagerId1, "MELE1", 1.0, "KG");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("L'elemento non può essere eliminato")));
    }

    @Test
    void givenAManagerOfOtherOrderType_whenDeletingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenDeletingOrder_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null);

        String orderId = createOrder(orderDTO);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/order/manage/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenDeletingOrder_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.delete("/api/order/manage/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenDueDateInThePast_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Data di chiusura non valida")));
    }

    @Test
    void givenDueDateEqualToOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(2), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDueDateBeforeOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(1), 10, LocalDate.now().plusDays(6), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateEqualsToDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(5), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenDeliveryDateBeforeDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Date ordine non valide")));
    }

    @Test
    void givenEmptyOrderTypeId_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(null, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyOpeningDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), null,
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDueDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                null, 10, LocalDate.now().plusDays(3), null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmptyDeliveryDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, null, null);

        mockMvcGoGas.post("/api/order/manage", orderDTO)
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenSecondOrderWithSameDueAndDeliveryDate_whenCreatingOrder_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(10), null);

        createOrder(orderDTO);

        OrderDTO duplicatedOrderDTO = buildValidOrderDTO(orderTypeComputed.getId(), LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5), 10, LocalDate.now().plusDays(10), null);

        mockMvcGoGas.post("/api/order/manage", duplicatedOrderDTO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Esiste già un ordine nello stesso periodo")));
    }

    private void addUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productsByCodeComputed.get(productCode).getId());
        request.setQuantity(BigDecimal.valueOf(qty));
        request.setUnitOfMeasure(um);

        mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    private OrderDTO buildValidOrderDTO(String orderTypeId) {
        return buildValidOrderDTO(orderTypeId, null);
    }

    private OrderDTO buildValidOrderDTO(String orderTypeId, String externalUrl) {
        return buildValidOrderDTO(orderTypeId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 14,
                LocalDate.now().plusDays(10), externalUrl);
    }

    private OrderDTO buildValidOrderDTO(String orderTypeId, LocalDate openingDate, LocalDate dueDate, int dueHour,
                                        LocalDate deliveryDate, String externalUrl) {

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderTypeId(orderTypeId);
        orderDTO.setOpeningDate(openingDate);
        orderDTO.setDueDate(dueDate);
        orderDTO.setDueHour(dueHour);
        orderDTO.setDeliveryDate(deliveryDate);
        orderDTO.setExternalLink(externalUrl);
        return orderDTO;
    }

    private String createOrder(OrderDTO orderDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.postDTO("/api/order/manage", orderDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        String orderId = creationResponse.getData().toString();
        createdOrderIds.add(orderId);
        return orderId;
    }

    private void verifyCreatedOrder(String orderId, String orderTypeId, String description, Integer aequosId, boolean external,
                                    String externalUrl, boolean computedAmount, boolean isSendWeightRequired) throws Exception {

        verifyCreatedOrder(orderId, orderTypeId, description, aequosId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 14, LocalDate.now().plusDays(10),
                "modifica,dettaglio,cancel", external, externalUrl, computedAmount, isSendWeightRequired);
    }

    private void verifyCreatedOrder(String orderId, String orderTypeId, String description, Integer aequosId,
                                    LocalDate openingDate, LocalDate dueDate, int dueHours, LocalDate deliveryDate, String actions,
                                    boolean external, String externalUrl, boolean computedAmount, boolean isSendWeightRequired) throws Exception {

        verifyOrder(orderId, orderTypeId, description, openingDate, dueDate, dueHours, deliveryDate, aequosId, 0, "Aperto",
                actions, 0, 0.0, false, external, externalUrl, computedAmount, 0.0, false, isSendWeightRequired, false);
    }

    private void verifyOrder(String orderId, String orderTypeId, String description, LocalDate openingDate, LocalDate dueDate, int dueHours, LocalDate deliveryDate,
                             Integer aequosId, int statusCode, String statusName, String actions, int itemsCount, double totalAmount, boolean editable, boolean external,
                             String externalUrl, boolean computedAmount, double shippingCosts, boolean accounted, boolean isSendWeightRequired, boolean isSendWeightAllowed) throws Exception {

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(orderTypeId, orderDTO.getOrderTypeId());
        assertEquals(description, orderDTO.getOrderTypeName());
        assertEquals(openingDate, orderDTO.getOpeningDate());
        assertEquals(dueDate, orderDTO.getDueDate());
        assertEquals(dueHours, orderDTO.getDueHour());
        assertEquals(deliveryDate, orderDTO.getDeliveryDate());
        assertEquals(statusName, orderDTO.getStatusName());
        assertEquals(statusCode, orderDTO.getStatusCode());
        assertEquals(externalUrl, orderDTO.getExternalLink());
        assertEquals(actions, orderDTO.getActions());
        assertEquals(itemsCount, orderDTO.getItemsCount());
        assertEquals(totalAmount, orderDTO.getTotalAmount().doubleValue(), 0.001);
        assertNull(orderDTO.getInvoiceAmount());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);

        assertNotNull(orderDetails);
        assertEquals(orderTypeId, orderDetails.getOrderTypeId());
        assertEquals(description, orderDetails.getOrderTypeName());
        assertEquals(deliveryDate, orderDetails.getDeliveryDate());
        assertEquals(aequosId, orderDetails.getAequosId());
        assertEquals(editable, orderDetails.isEditable());
        assertEquals(external, orderDetails.isExternal());
        assertEquals(computedAmount, orderDetails.isComputedAmount());
        assertEquals(shippingCosts, orderDetails.getShippingCost().doubleValue(), 0.0001);
        assertEquals(accounted, orderDetails.isAccounted());
        assertNull(orderDetails.getInvoiceNumber());
        assertNull(orderDetails.getInvoiceAmount());
        assertNull(orderDetails.getInvoiceDate());
        assertFalse(orderDetails.isPaid());
        assertNull(orderDetails.getPaymentDate());
        assertEquals(isSendWeightRequired, orderDetails.isSendWeightsRequired());
        assertEquals(isSendWeightAllowed, orderDetails.isSendWeightsAllowed());
        assertFalse(orderDetails.isHasAttachment());
        assertFalse(orderDetails.isSent());
        assertNull(orderDetails.getExternalOrderId());
        assertNull(orderDetails.getSyncDate());
    }
}
