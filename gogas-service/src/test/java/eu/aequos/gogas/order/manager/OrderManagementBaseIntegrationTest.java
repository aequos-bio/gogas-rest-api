package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.persistence.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderManagementBaseIntegrationTest extends BaseGoGasIntegrationTest {

    protected OrderType orderTypeComputed;
    protected OrderType orderTypeNotComputed;
    protected OrderType orderTypeExternal;
    protected OrderType orderTypeAequos;

    protected Map<String, Product> productsByCodeComputed;
    protected Map<String, Product> productsByCodeNotComputed;

    protected String userId1;
    protected String userId2;
    protected String userId3;
    protected String friendId1a;
    protected String friendId1b;
    protected String friendId2;
    protected String orderManagerId1;
    protected String orderManagerId2;

    protected List<String> createdOrderIds;

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

        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");
        User user3 = mockUsersData.createSimpleUser("user3", "password", "user3", "user3");

        userId1 = user1.getId().toUpperCase();
        userId2 = user2.getId().toUpperCase();
        userId3 = user3.getId().toUpperCase();

        friendId1a = mockUsersData.createFriendUser("friendId1a", "password", "friendId1a", "friendId1a", user1).getId().toUpperCase();
        friendId1b = mockUsersData.createFriendUser("friendId1b", "password", "friendId1b", "friendId1b", user1).getId().toUpperCase();
        friendId2 = mockUsersData.createFriendUser("friendId2", "password", "friendId2", "friendId2", user2).getId().toUpperCase();

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, orderTypeComputed);
        mockOrdersData.addManager(orderManager, orderTypeExternal);
        orderManagerId1 = orderManager.getId();

        User otherManager = mockUsersData.createSimpleUser("manager2", "password", "manager2", "manager2");
        mockOrdersData.addManager(otherManager, orderTypeNotComputed);
        mockOrdersData.addManager(otherManager, orderTypeAequos);
        orderManagerId2 = otherManager.getId();
    }

    @BeforeEach
    void setUp() {
        createdOrderIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        createdOrderIds.forEach(mockOrdersData::deleteOrder);
    }

    void performAction(String orderId, String action) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/" + action, null, BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    void performClose(String orderId, int roundType) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/close", null, BasicResponseDTO.class, Map.of("roundType", List.of(String.valueOf(roundType))));
        assertEquals("OK", cancelResponse.getData());
    }

    void invalidAction(String orderId, String action) throws Exception {
        invalidAction(orderId, action, "Invalid order status");
    }

    void invalidAction(String orderId, String action, String errorMessage) throws Exception {
        mockMvcGoGas.post("/api/order/manage/" + orderId + "/action/" + action, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(errorMessage)));
    }

    List<UserOrderItemDTO> getUserOpenOrderItems(String orderId, String userId) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/user/" + orderId + "/items", UserOrderItemDTO.class, Map.of("userId", List.of(userId))).stream()
                .filter(item -> item.getOrderRequestedQty() != null)
                .collect(Collectors.toList());
    }

    void addComputedUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        addUserOrder(orderId, userId, productsByCodeComputed.get(productCode).getId(), qty, um);
    }

    void addNotComputedUserOrder(String orderId, String userId, String productCode, double qty, String um) throws Exception {
        addUserOrder(orderId, userId, productsByCodeNotComputed.get(productCode).getId(), qty, um);
    }

    private void addUserOrder(String orderId, String userId, String productId, double qty, String um) throws Exception {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productId);
        request.setQuantity(BigDecimal.valueOf(qty));
        request.setUnitOfMeasure(um);

        mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    void setUserCost(String orderId, String userId, double amount) throws Exception {
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/byUser/" + userId, BigDecimal.valueOf(amount), BasicResponseDTO.class);
        assertNotNull(updateResponse);
    }

    OrderDTO buildValidOrderDTO(String orderTypeId) {
        return buildValidOrderDTO(orderTypeId, null);
    }

    OrderDTO buildValidOrderDTO(String orderTypeId, String externalUrl) {
        return buildOrderDTO(orderTypeId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 14,
                LocalDate.now().plusDays(10), externalUrl);
    }

    OrderDTO buildOrderDTO(String orderTypeId, LocalDate openingDate, LocalDate dueDate, int dueHour,
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

    String createOrder(OrderDTO orderDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.postDTO("/api/order/manage", orderDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        String orderId = creationResponse.getData().toString();
        createdOrderIds.add(orderId);
        return orderId;
    }

    void verifyOrderStatus(String orderId, String orderTypeId, int statusCode) throws Exception {
        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(statusCode, orderDTO.getStatusCode());
    }

    void verifyOrderStatusAndActions(String orderId, String orderTypeId, int statusCode, String statusName, String actions,
                             boolean editable, boolean accounted) throws Exception {

        OrderSearchFilter searchFilter = new OrderSearchFilter();
        searchFilter.setOrderType(orderTypeId);

        Optional<OrderDTO> orderFromSearch = mockMvcGoGas.postDTOList("/api/order/manage/list", searchFilter, OrderDTO.class).stream()
                .filter(orderDTO -> orderDTO.getId().equals(orderId.toUpperCase()))
                .findFirst();

        assertTrue(orderFromSearch.isPresent());
        OrderDTO orderDTO = orderFromSearch.get();
        assertEquals(statusName, orderDTO.getStatusName());
        assertEquals(statusCode, orderDTO.getStatusCode());
        assertEquals(actions, orderDTO.getActions());

        OrderDetailsDTO orderDetails = mockMvcGoGas.getDTO("/api/order/manage/" + orderId, OrderDetailsDTO.class);

        assertNotNull(orderDetails);
        assertEquals(editable, orderDetails.isEditable());
        assertEquals(accounted, orderDetails.isAccounted());
    }

    Map<String, OrderItemByProductDTO> getProductItems(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product/" + productsByCodeComputed.get(productCode).getId(), OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    Map<String, OrderItemByProductDTO> getNotComputedProductItems(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product/" + productsByCodeNotComputed.get(productCode).getId(), OrderItemByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderItemByProductDTO::getUserId, Function.identity()));
    }

    OrderByProductDTO getProductOrder(String orderId, String productCode) throws Exception {
        return mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .filter(product -> product.getProductId().equals(productsByCodeComputed.get(productCode).getId().toUpperCase()))
                .findFirst()
                .orElse(null);
    }

    void changeUserOrderItemQuantity(String orderId, String productCode, String userId, double quantity) throws Exception {
        Map<String, OrderItemByProductDTO> items = getProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + items.get(userId).getOrderItemId(), BigDecimal.valueOf(quantity), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void changeUserOrderItemQuantityNotComputed(String orderId, String productCode, String userId, double quantity) throws Exception {
        Map<String, OrderItemByProductDTO> items = getNotComputedProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + items.get(userId).getOrderItemId(), BigDecimal.valueOf(quantity), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void cancelProductOrder(String orderId, String productCode) throws Exception {
        BasicResponseDTO cancelResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/product/" + productsByCodeComputed.get(productCode).getId() + "/cancel", BasicResponseDTO.class);
        assertEquals("OK", cancelResponse.getData());
    }

    void cancelUserOrder(String orderId, String userId, String productCode) throws Exception {
        Map<String, OrderItemByProductDTO> itemsBefore = getProductItems(orderId, productCode);

        BasicResponseDTO updateQtaResponse = mockMvcGoGas.putDTO("/api/order/manage/" + orderId + "/item/" + itemsBefore.get(userId).getOrderItemId(), BigDecimal.valueOf(3.0), BasicResponseDTO.class);
        assertEquals("OK", updateQtaResponse.getData());
    }

    void verifyOrderItem(OrderItemByProductDTO item, double requestedQty, double deliveredQty, String um, boolean cancelled) {
        assertEquals(um, item.getUnitOfMeasure());
        assertEquals(requestedQty, item.getRequestedQty().doubleValue(), 0.001);
        assertEquals(deliveredQty, item.getDeliveredQty().doubleValue(), 0.001);
        assertEquals(cancelled, item.isCancelled());
    }

    void verifyOrderOpenedItem(OrderItemByProductDTO item, double requestedQty, String um, boolean cancelled) {
        assertEquals(um, item.getUnitOfMeasure());
        assertEquals(requestedQty, item.getRequestedQty().doubleValue(), 0.001);
        assertNull(item.getDeliveredQty());
        assertEquals(cancelled, item.isCancelled());
    }
}
