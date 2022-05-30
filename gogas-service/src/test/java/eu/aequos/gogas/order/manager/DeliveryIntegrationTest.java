package eu.aequos.gogas.order.manager;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderByProductDTO;
import eu.aequos.gogas.dto.OrderItemByProductDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderItemDTO;
import eu.aequos.gogas.dto.delivery.DeliveryProductDTO;
import eu.aequos.gogas.persistence.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DeliveryIntegrationTest extends OrderManagementBaseIntegrationTest {

    private String orderId;

    @BeforeEach
    void createOrder() {
        Order order = mockOrdersData.createOrder(orderTypeComputed, LocalDate.now().minusDays(3), LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(5), Order.OrderStatus.Opened.getStatusCode(), BigDecimal.ZERO);

        orderId = order.getId();
    }

    @Test
    void givenAClosedOrderComputedAmountWithUsersOnly_whenGettingDeliveryData_thenDataIsCorrect() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 4.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 7.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        addNewUserOrderItem(orderId, "BIRRA1", userId3, 3.0, "PZ");
        cancelUserOrder(orderId, userId1, "BIRRA1");

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        assertEquals(LocalDate.now().plusDays(7), deliveryData.getDeliveryDate());
        assertEquals(orderId, deliveryData.getOrderId());
        assertEquals("Fresco Settimanale", deliveryData.getOrderType());
        assertEquals(mockUsersData.getAllUsers(false), deliveryData.getUsers().stream().map(UserDTO::getUsername).collect(Collectors.toSet()));

        Map<String, DeliveryProductDTO> productsById = deliveryData.getProducts().stream()
                .collect(Collectors.toMap(DeliveryProductDTO::getProductId, Function.identity()));

        DeliveryProductDTO product1 = productsById.get(productsByCodeComputed.get("MELE1").getId().toUpperCase());
        assertEquals(1.55, product1.getPrice().doubleValue(), 0.0001);
        assertEquals("MELE CRIMSON CRISP - Roncaglia", product1.getProductName());
        assertEquals("KG", product1.getUnitOfMeasure());
        assertEquals(8.5 , product1.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(2 , product1.getOrderedBoxes().intValue());

        assertEquals(3, product1.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items1ByUser = product1.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items1ByUser.get(userId1), 3.0, 4.0, 4.0);
        verifyItemQuantities(items1ByUser.get(userId2), 8.5, 8.5, 8.5);
        verifyItemQuantities(items1ByUser.get(userId3), 3.5, 3.5, 3.5);

        DeliveryProductDTO product2 = productsById.get(productsByCodeComputed.get("PATATE").getId().toUpperCase());
        assertEquals(1.45, product2.getPrice().doubleValue(), 0.0001);
        assertEquals("PATATE GIALLE DI MONTAGNA - Abbiate Valerio", product2.getProductName());
        assertEquals("KG", product2.getUnitOfMeasure());
        assertEquals(11.0 , product2.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(1 , product2.getOrderedBoxes().intValue());

        assertEquals(3, product2.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items2ByUser = product2.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items2ByUser.get(userId1), 4.0, 4.0, 4.0);
        verifyItemQuantities(items2ByUser.get(userId2), 4.5, 7.0, 7.0);
        verifyItemQuantities(items2ByUser.get(userId3), 5.5, 3.35, 3.35);

        DeliveryProductDTO product3 = productsById.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase());
        assertEquals(3.65, product3.getPrice().doubleValue(), 0.0001);
        assertEquals("BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone", product3.getProductName());
        assertEquals("PZ", product3.getUnitOfMeasure());
        assertEquals(1.0 , product3.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(3 , product3.getOrderedBoxes().intValue());

        assertEquals(3, product3.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items3ByUser = product3.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items3ByUser.get(userId1), 1.0, 0.0, 0.0);
        verifyItemQuantities(items3ByUser.get(userId2), 2.0, 2.0, 2.0);
        verifyItemQuantities(items3ByUser.get(userId3), 0.0, 3.0, 3.0);
    }

    @Test
    void givenAClosedOrderComputedAmountWithFriendsAndSummary_whenGettingDeliveryData_thenDataIsCorrect() throws Exception {
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);

        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 4.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 7.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        addNewUserOrderItem(orderId, "BIRRA1", userId3, 3.0, "PZ");
        cancelUserOrder(orderId, userId1, "BIRRA1");

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        assertEquals(LocalDate.now().plusDays(7), deliveryData.getDeliveryDate());
        assertEquals(orderId, deliveryData.getOrderId());
        assertEquals("Fresco Settimanale", deliveryData.getOrderType());
        assertEquals(mockUsersData.getAllUsers(true), deliveryData.getUsers().stream().map(UserDTO::getUsername).collect(Collectors.toSet()));

        Map<String, DeliveryProductDTO> productsById = deliveryData.getProducts().stream()
                .collect(Collectors.toMap(DeliveryProductDTO::getProductId, Function.identity()));

        DeliveryProductDTO product1 = productsById.get(productsByCodeComputed.get("MELE1").getId().toUpperCase());
        assertEquals(1.55, product1.getPrice().doubleValue(), 0.0001);
        assertEquals("MELE CRIMSON CRISP - Roncaglia", product1.getProductName());
        assertEquals("KG", product1.getUnitOfMeasure());
        assertEquals(8.5 , product1.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(2 , product1.getOrderedBoxes().intValue());

        assertEquals(3, product1.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items1ByUser = product1.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items1ByUser.get(userId1), 5.5, 4.0, 4.0);
        verifyItemQuantities(items1ByUser.get(userId2), 8.5, 8.5, 8.5);
        verifyItemQuantities(items1ByUser.get(userId3), 3.5, 3.5, 3.5);

        DeliveryProductDTO product2 = productsById.get(productsByCodeComputed.get("PATATE").getId().toUpperCase());
        assertEquals(1.45, product2.getPrice().doubleValue(), 0.0001);
        assertEquals("PATATE GIALLE DI MONTAGNA - Abbiate Valerio", product2.getProductName());
        assertEquals("KG", product2.getUnitOfMeasure());
        assertEquals(11.0 , product2.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(2 , product2.getOrderedBoxes().intValue());

        assertEquals(3, product2.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items2ByUser = product2.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items2ByUser.get(userId1), 6.5, 6.5, 6.5);
        verifyItemQuantities(items2ByUser.get(userId2), 4.5, 7.0, 7.0);
        verifyItemQuantities(items2ByUser.get(userId3), 5.5, 3.35, 3.35);

        DeliveryProductDTO product3 = productsById.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase());
        assertEquals(3.65, product3.getPrice().doubleValue(), 0.0001);
        assertEquals("BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone", product3.getProductName());
        assertEquals("PZ", product3.getUnitOfMeasure());
        assertEquals(1.0 , product3.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(6 , product3.getOrderedBoxes().intValue());

        assertEquals(3, product3.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items3ByUser = product3.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items3ByUser.get(userId1), 3.0, 0.0, 0.0);
        verifyItemQuantities(items3ByUser.get(userId2), 3.0, 3.0, 3.0);
        verifyItemQuantities(items3ByUser.get(userId3), 0.0, 3.0, 3.0);

        DeliveryProductDTO product4 = productsById.get(productsByCodeComputed.get("MELE2").getId().toUpperCase());
        assertEquals(1.70, product4.getPrice().doubleValue(), 0.0001);
        assertEquals("MELE OPAL - Roncaglia", product4.getProductName());
        assertEquals("KG", product4.getUnitOfMeasure());
        assertEquals(8.5 , product4.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(1 , product4.getOrderedBoxes().intValue());

        assertEquals(2, product4.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items4ByUser = product4.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items4ByUser.get(userId1), 4.0, 4.0, 4.0);
        verifyItemQuantities(items4ByUser.get(userId2), 8.5, 8.5, 8.5);

        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);
    }

    @Test
    void givenAClosedOrderComputedAmountWithFriendsAndNOSummary_whenGettingDeliveryData_thenDataIsCorrect() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        changeUserOrderItemQuantity(orderId, "MELE1", userId1, 4.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId2, 7.0);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        changeUserOrderItemQuantity(orderId, "PATATE", userId3, 3.35);
        addNewUserOrderItem(orderId, "BIRRA1", userId3, 3.0, "PZ");
        cancelUserOrder(orderId, userId1, "BIRRA1");

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        assertEquals(LocalDate.now().plusDays(7), deliveryData.getDeliveryDate());
        assertEquals(orderId, deliveryData.getOrderId());
        assertEquals("Fresco Settimanale", deliveryData.getOrderType());
        assertEquals(mockUsersData.getAllUsers(false), deliveryData.getUsers().stream().map(UserDTO::getUsername).collect(Collectors.toSet()));

        Map<String, DeliveryProductDTO> productsById = deliveryData.getProducts().stream()
                .collect(Collectors.toMap(DeliveryProductDTO::getProductId, Function.identity()));

        DeliveryProductDTO product1 = productsById.get(productsByCodeComputed.get("MELE1").getId().toUpperCase());
        assertEquals(1.55, product1.getPrice().doubleValue(), 0.0001);
        assertEquals("MELE CRIMSON CRISP - Roncaglia", product1.getProductName());
        assertEquals("KG", product1.getUnitOfMeasure());
        assertEquals(8.5 , product1.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(2 , product1.getOrderedBoxes().intValue());

        assertEquals(4, product1.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items1ByUser = product1.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items1ByUser.get(userId1), 3.5, 4.0, 4.0);
        verifyItemQuantities(items1ByUser.get(friendId1b), 2.0, 2.0, 2.0);
        verifyItemQuantities(items1ByUser.get(userId2), 8.5, 8.5, 8.5);
        verifyItemQuantities(items1ByUser.get(userId3), 3.5, 3.5, 3.5);

        DeliveryProductDTO product2 = productsById.get(productsByCodeComputed.get("PATATE").getId().toUpperCase());
        assertEquals(1.45, product2.getPrice().doubleValue(), 0.0001);
        assertEquals("PATATE GIALLE DI MONTAGNA - Abbiate Valerio", product2.getProductName());
        assertEquals("KG", product2.getUnitOfMeasure());
        assertEquals(11.0 , product2.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(2 , product2.getOrderedBoxes().intValue());

        assertEquals(4, product2.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items2ByUser = product2.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items2ByUser.get(userId1), 4.0, 4.0, 4.0);
        verifyItemQuantities(items2ByUser.get(friendId1b), 2.5, 2.5, 2.5);
        verifyItemQuantities(items2ByUser.get(userId2), 4.5, 7.0, 7.0);
        verifyItemQuantities(items2ByUser.get(userId3), 5.5, 3.35, 3.35);

        DeliveryProductDTO product3 = productsById.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase());
        assertEquals(3.65, product3.getPrice().doubleValue(), 0.0001);
        assertEquals("BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone", product3.getProductName());
        assertEquals("PZ", product3.getUnitOfMeasure());
        assertEquals(1.0 , product3.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(6 , product3.getOrderedBoxes().intValue());

        assertEquals(5, product3.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items3ByUser = product3.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items3ByUser.get(userId1), 1.0, 0.0, 0.0);
        verifyItemQuantities(items3ByUser.get(friendId1a), 2.0, 2.0, 2.0);
        verifyItemQuantities(items3ByUser.get(userId2), 2.0, 2.0, 2.0);
        verifyItemQuantities(items3ByUser.get(friendId2), 1.0, 1.0, 1.0);
        verifyItemQuantities(items3ByUser.get(userId3), 0.0, 3.0, 3.0);

        DeliveryProductDTO product4 = productsById.get(productsByCodeComputed.get("MELE2").getId().toUpperCase());
        assertEquals(1.70, product4.getPrice().doubleValue(), 0.0001);
        assertEquals("MELE OPAL - Roncaglia", product4.getProductName());
        assertEquals("KG", product4.getUnitOfMeasure());
        assertEquals(8.5 , product4.getBoxWeight().doubleValue(), 0.0001);
        assertEquals(1 , product4.getOrderedBoxes().intValue());

        assertEquals(2, product4.getOrderItems().size());

        Map<String, DeliveryOrderItemDTO> items4ByUser = product4.getOrderItems().stream()
                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()));

        verifyItemQuantities(items4ByUser.get(friendId1a), 4.0, 4.0, 4.0);
        verifyItemQuantities(items4ByUser.get(friendId2), 8.5, 8.5, 8.5);
    }

    @Test
    void givenModifiedQuantities_whenUploadingDeliveryData_thenOrdersAreCorrectlyChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 2.754);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.072);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 8.572);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.395);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.286);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId3, 4.768);

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/delivery/" + orderId, deliveryData, BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(14.612, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(13.235, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(2.754, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.572, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.286, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.072, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.395, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.768, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(0.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenRemovedQuantities_whenUploadingDeliveryData_thenOrdersAreCorrectlyChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        deleteDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1);

        deleteDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2);

        deleteDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId3, 4.768);

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/delivery/" + orderId, deliveryData, BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(0.0, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(13.268, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(0.0, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(0.0, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(0.0, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.768, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(0.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAddedQuantitiesAndUser_whenUploadingDeliveryData_thenOrdersAreCorrectlyChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 4.735);
        addDeliveredQuantity(deliveryData, "MELE1", orderManagerId1.toUpperCase(), 2.593);
        addDeliveredQuantity(deliveryData, "BIRRA1", userId3, 5.0);

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/delivery/" + orderId, deliveryData, BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(19.328, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(14.0, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(4.735, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.5, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.5, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.593, orderItems1.get(orderManagerId1.toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.0, orderItems3.get(userId3).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAnOrderWithFriendsAndNOSummaryAndAddedQuantitiesAndUser_whenUploadingDeliveryData_thenDataIsCorrectlyChanged() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 4.735);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), friendId1b, 2.034);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 7.624);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.881);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE2").getId(), friendId1a, 3.993);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE2").getId(), friendId2, 8.923);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.098);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), friendId1b, 2.699);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.416);

        addDeliveredQuantity(deliveryData, "MELE1", friendId1a, 1.470);
        addDeliveredQuantity(deliveryData, "MELE2", orderManagerId1.toUpperCase(), 2.593);
        addDeliveredQuantity(deliveryData, "MELE2", friendId1b, 1.249);
        addDeliveredQuantity(deliveryData, "PATATE", friendId2, 2.004);


        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/delivery/" + orderId, deliveryData, BasicResponseDTO.class);
        assertEquals("OK", basicResponseDTO.getData());

        Map<String, OrderByProductDTO> productsById = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(19.744, productsById.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(16.758, productsById.get(productsByCodeComputed.get("MELE2").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(18.717, productsById.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(6.0, productsById.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(4.735, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(1.470, orderItems1.get(friendId1a).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.034, orderItems1.get(friendId1b).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(7.624, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.881, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "MELE2");
        assertEquals(3.993, orderItems2.get(friendId1a).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(1.249, orderItems2.get(friendId1b).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.923, orderItems2.get(friendId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.593, orderItems2.get(orderManagerId1.toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "PATATE");
        assertEquals(4.098, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.699, orderItems3.get(friendId1b).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.416, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.004, orderItems3.get(friendId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems3.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems4 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems4.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems4.get(friendId1a).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems4.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(1.0, orderItems4.get(friendId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAnInvalidOrder_whenGettingDeliveryData_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        mockMvcGoGas.get("/api/delivery/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUser_whenGettingDeliveryData_thenForbiddenIsReturned() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        mockMvcGoGas.loginAs("user1", "password");
        mockMvcGoGas.get("/api/delivery/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenGettingDeliveryData_thenForbiddenIsReturned() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        mockMvcGoGas.get("/api/delivery/" + orderId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnInvalidOrder_whenUploadingDeliveryData_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        mockMvcGoGas.post("/api/delivery/" + UUID.randomUUID(), new DeliveryOrderDTO())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASimpleUser_whenUploadingDeliveryData_thenForbiddenIsReturned() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        mockMvcGoGas.loginAs("user1", "password");
        mockMvcGoGas.post("/api/delivery/" + orderId, new DeliveryOrderDTO())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAManagerOfOtherOrderType_whenUploadingDeliveryData_thenForbiddenIsReturned() throws Exception {
        addComputedUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        mockMvcGoGas.loginAs("manager2", "password");
        mockMvcGoGas.post("/api/delivery/" + orderId, new DeliveryOrderDTO())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnInvalidOrderIdInPayload_whenUploadingDeliveryData_thenDataIsNotChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 2.754);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.072);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 8.572);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.395);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.286);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId3, 4.768);

        deliveryData.setOrderId(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/delivery/" + orderId, deliveryData)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Order id is not valid")));

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(15.0, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(14.0, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(3.0, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.5, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.5, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAnInvalidProductIdInPayload_whenUploadingDeliveryData_thenDataIsNotChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 2.754);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.072);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 8.572);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.395);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.286);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId3, 4.768);

        String invalidProductId = UUID.randomUUID().toString();

        DeliveryOrderItemDTO deliveryOrderItemDTO = new DeliveryOrderItemDTO();
        deliveryOrderItemDTO.setUserId(userId1);
        deliveryOrderItemDTO.setFinalDeliveredQty(BigDecimal.ONE);
        deliveryOrderItemDTO.setChanged(true);

        DeliveryProductDTO deliveryProductDTO = new DeliveryProductDTO();
        deliveryProductDTO.setProductId(invalidProductId);
        deliveryProductDTO.setOrderItems(List.of(deliveryOrderItemDTO));

        deliveryData.getProducts().add(deliveryProductDTO);

        mockMvcGoGas.post("/api/delivery/" + orderId, deliveryData)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Item not found. Type: product, Id: " + invalidProductId)));

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(15.0, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(14.0, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(3.0, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.5, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.5, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAProductIdOfDifferentOrderInPayload_whenUploadingDeliveryData_thenDataIsNotChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 2.754);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.072);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 8.572);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.395);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.286);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId3, 4.768);

        DeliveryOrderItemDTO deliveryOrderItemDTO = new DeliveryOrderItemDTO();
        deliveryOrderItemDTO.setUserId(userId1);
        deliveryOrderItemDTO.setFinalDeliveredQty(BigDecimal.ONE);
        deliveryOrderItemDTO.setChanged(true);

        DeliveryProductDTO deliveryProductDTO = new DeliveryProductDTO();
        deliveryProductDTO.setProductId(productsByCodeNotComputed.get("COSTINE").getId());
        deliveryProductDTO.setOrderItems(List.of(deliveryOrderItemDTO));

        deliveryData.getProducts().add(deliveryProductDTO);

        mockMvcGoGas.post("/api/delivery/" + orderId, deliveryData)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Product id " + productsByCodeNotComputed.get("COSTINE").getId() + " is not valid")));

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(15.0, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(14.0, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(3.0, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.5, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.5, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    @Test
    void givenAnInvalidUserId_whenUploadingDeliveryData_thenDataIsNotChanged() throws Exception {
        addComputedUserOrdersNoFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        closeOrder(orderId);

        DeliveryOrderDTO deliveryData = mockMvcGoGas.getDTO("/api/delivery/" + orderId, DeliveryOrderDTO.class);

        Map<String, Map<String, DeliveryOrderItemDTO>> deliveryDataMap = extractDeliveryOrderItemDTO(deliveryData);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("BIRRA1").getId(), userId1, 0.0);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId1, 2.754);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId1, 4.072);

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId2, 8.572);
        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("PATATE").getId(), userId2, 4.395);

        String invalidUserId = UUID.randomUUID().toString();

        changeDeliveredQuantity(deliveryDataMap, productsByCodeComputed.get("MELE1").getId(), userId3, 3.286);
        addDeliveredQuantity(deliveryData, "PATATE", invalidUserId, 4.768);

        mockMvcGoGas.post("/api/delivery/" + orderId, deliveryData)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Item not found. Type: user, Id: " + invalidUserId)));

        Map<String, OrderByProductDTO> products = mockMvcGoGas.getDTOList("/api/order/manage/" + orderId + "/product", OrderByProductDTO.class).stream()
                .collect(Collectors.toMap(OrderByProductDTO::getProductId, Function.identity()));

        assertEquals(15.0, products.get(productsByCodeComputed.get("MELE1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(14.0, products.get(productsByCodeComputed.get("PATATE").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.0, products.get(productsByCodeComputed.get("BIRRA1").getId().toUpperCase()).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems1 = getProductItems(orderId, "MELE1");
        assertEquals(3.0, orderItems1.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(8.5, orderItems1.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(3.5, orderItems1.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems2 = getProductItems(orderId, "PATATE");
        assertEquals(4.0, orderItems2.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(4.5, orderItems2.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(5.5, orderItems2.get(userId3).getDeliveredQty().doubleValue(), 0.0001);

        Map<String, OrderItemByProductDTO> orderItems3 = getProductItems(orderId, "BIRRA1");
        assertEquals(1.0, orderItems3.get(userId1).getDeliveredQty().doubleValue(), 0.0001);
        assertEquals(2.0, orderItems3.get(userId2).getDeliveredQty().doubleValue(), 0.0001);
    }

    private void changeDeliveredQuantity(Map<String, Map<String, DeliveryOrderItemDTO>> deliveryData, String productCode, String userId, double quantity) {
        DeliveryOrderItemDTO deliveryItem = deliveryData.get(productCode.toUpperCase()).get(userId);
        deliveryItem.setFinalDeliveredQty(BigDecimal.valueOf(quantity));
        deliveryItem.setChanged(true);
    }

    private void deleteDeliveredQuantity(Map<String, Map<String, DeliveryOrderItemDTO>> deliveryData, String productCode, String userId) {
        DeliveryOrderItemDTO deliveryItem = deliveryData.get(productCode.toUpperCase()).get(userId);
        deliveryItem.setFinalDeliveredQty(null);
        deliveryItem.setChanged(true);
    }

    private void addDeliveredQuantity(DeliveryOrderDTO deliveryData, String productCode, String userId, double quantity) {
        DeliveryOrderItemDTO deliveredItem = new DeliveryOrderItemDTO();
        deliveredItem.setUserId(userId);
        deliveredItem.setFinalDeliveredQty(BigDecimal.valueOf(quantity));
        deliveredItem.setChanged(true);

        deliveryData.getProducts().stream()
                .filter(product -> product.getProductId().equalsIgnoreCase(productsByCodeComputed.get(productCode).getId()))
                .findAny()
                .ifPresent(product -> product.getOrderItems().add(deliveredItem));
    }

    private Map<String, Map<String, DeliveryOrderItemDTO>> extractDeliveryOrderItemDTO(DeliveryOrderDTO deliveryData) {
        return deliveryData.getProducts().stream()
                .collect(Collectors.toMap(
                        DeliveryProductDTO::getProductId,
                        p -> p.getOrderItems().stream()
                                .collect(Collectors.toMap(DeliveryOrderItemDTO::getUserId, Function.identity()))
                ));
    }

    private void verifyItemQuantities(DeliveryOrderItemDTO item, Double requested, Double original, Double finalQty) {
        assertEquals(requested, item.getRequestedQty().doubleValue(), 0.0001);
        assertEquals(original, item.getOriginalDeliveredQty().doubleValue(), 0.0001);
        assertEquals(finalQty, item.getFinalDeliveredQty().doubleValue(), 0.0001);
        assertFalse(item.isChanged());
    }

    private void closeOrder(String orderId) throws Exception {
        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1),LocalDate.now().plusDays(7));
        performAction(orderId, "close");
    }

    private void addComputedUserOrdersNoFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.0, "KG");
        addComputedUserOrder(orderId, userId1, "PATATE", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRA1", 1.0, "PZ");

        mockMvcGoGas.loginAs("user2", "password");
        addComputedUserOrder(orderId, userId2, "MELE1", 1.0, "Cassa");
        addComputedUserOrder(orderId, userId2, "PATATE", 4.5, "KG");
        addComputedUserOrder(orderId, userId2, "BIRRA1", 2.0, "PZ");

        mockMvcGoGas.loginAs("user3", "password");
        addComputedUserOrder(orderId, userId3, "MELE1", 3.5, "KG");
        addComputedUserOrder(orderId, userId3, "PATATE", 5.5, "KG");
    }

    private void addComputedUserOrdersWithFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.5, "KG");
        addComputedUserOrder(orderId, userId1, "PATATE", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRA1", 1.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "BIRRA1", 2.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "MELE2", 4.0, "KG");
        addComputedUserOrder(orderId, friendId1b, "MELE1", 2.0, "KG");
        addComputedUserOrder(orderId, friendId1b, "PATATE", 2.5, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        addComputedUserOrder(orderId, userId2, "MELE1", 1.0, "Cassa");
        addComputedUserOrder(orderId, userId2, "PATATE", 4.5, "KG");
        addComputedUserOrder(orderId, userId2, "BIRRA1", 2.0, "PZ");
        addComputedUserOrder(orderId, friendId2, "MELE2", 1.0, "Cassa");
        addComputedUserOrder(orderId, friendId2, "BIRRA1", 1.0, "PZ");

        mockMvcGoGas.loginAs("user3", "password");
        addComputedUserOrder(orderId, userId3, "MELE1", 3.5, "KG");
        addComputedUserOrder(orderId, userId3, "PATATE", 5.5, "KG");
    }

    private void checkBalance(String userId1, double expectedBalance) throws Exception {
        BigDecimal balance = mockMvcGoGas.getDTO("/api/accounting/user/" + userId1 + "/balance", BigDecimal.class);
        assertEquals(expectedBalance, balance.doubleValue(), 0.001);
    }
}
