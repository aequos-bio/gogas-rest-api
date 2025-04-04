package eu.aequos.gogas.integration;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.AequosOpenOrder;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosAvailableOrdersIntegrationTest extends BaseGoGasIntegrationTest {

    @MockBean
    private AequosApiClient aequosApiClient;

    private User orderManager;

    @BeforeAll
    void beforeAll() {
        orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
    }

    @BeforeEach
    void setUp() {
        when(aequosApiClient.openOrders()).thenReturn(Arrays.asList(
                buildOpenOrder(0, "Fresco Settimanale", "2022-03-20", "2022-03-24", "2022-03-30"),
                buildOpenOrder(1, "Pane", "2022-03-21", "2022-03-30", "2022-04-10"),
                buildOpenOrder(2, "Carni Bianche", "2022-03-21", "2022-03-26", "2022-04-05")
        ));
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenRequestingAequosAvailableOrders_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.get("/api/order/manage/aequos/available")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoExistingOrderTypesForAequosOrders_whenRequestingAequosAvailableOrders_thenNoOrderIsReturned() throws Exception {
        OrderType nonAequosType = mockOrdersData.createOrderType("Non Aequos Type");
        mockOrdersData.addManager(orderManager, nonAequosType);

        mockMvcGoGas.loginAs("manager", "password");

        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);
        assertTrue(availableOrders.isEmpty());
    }

    @Test
    void givenPartialExistingOrderTypesForAequosOrders_whenRequestingAequosAvailableOrders_thenOnlyExistingOrderTypesAreReturned() throws Exception {
        OrderType availableOrderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        OrderType otherOrderType = mockOrdersData.createAequosOrderType("Mozzarelle", 10);

        mockOrdersData.addManager(orderManager, availableOrderType);
        mockOrdersData.addManager(orderManager, otherOrderType);

        mockMvcGoGas.loginAs("manager", "password");

        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        List<OrderDTO> expectedOrders = List.of(buildExpectedOrderDTO(availableOrderType, "2022-03-20", "2022-03-24", "2022-03-30"));

        assertEquals(expectedOrders, availableOrders);
    }

    @Test
    void givenAllExistingOrderTypesForAequosOrders_whenRequestingAequosAvailableOrders_thenAllOrderTypesAreReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        existingOrderTypes.values().forEach(orderType -> mockOrdersData.addManager(orderManager, orderType));

        mockMvcGoGas.loginAs("manager", "password");
        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        List<OrderDTO> expectedOrders = List.of(
                buildExpectedOrderDTO(existingOrderTypes.get(0), "2022-03-20", "2022-03-24", "2022-03-30"),
                buildExpectedOrderDTO(existingOrderTypes.get(1), "2022-03-21", "2022-03-30", "2022-04-10"),
                buildExpectedOrderDTO(existingOrderTypes.get(2), "2022-03-21", "2022-03-26", "2022-04-05")
        );

        assertEquals(expectedOrders, availableOrders);
    }

    @Test
    void givenAdminUser_whenRequestingAequosAvailableOrders_thenNoOrderTypeIsReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        existingOrderTypes.values().forEach(orderType -> mockOrdersData.addManager(orderManager, orderType));

        mockMvcGoGas.loginAsAdmin();
        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        assertThat(availableOrders).isEmpty();
    }

    @Test
    void givenAdminUserAsOrderManager_whenRequestingAequosAvailableOrders_thenOrderTypesAreReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        String adminId = mockUsersData.getDefaultAdminId();
        mockOrdersData.addManager(adminId, existingOrderTypes.get(0));
        mockOrdersData.addManager(adminId, existingOrderTypes.get(2));

        mockMvcGoGas.loginAsAdmin();
        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        List<OrderDTO> expectedOrders = List.of(
                buildExpectedOrderDTO(existingOrderTypes.get(0), "2022-03-20", "2022-03-24", "2022-03-30"),
                buildExpectedOrderDTO(existingOrderTypes.get(2), "2022-03-21", "2022-03-26", "2022-04-05")
        );

        assertEquals(expectedOrders, availableOrders);
    }

    @Test
    void givenAnUserManagingSomeOfOrderTypes_whenRequestingAequosAvailableOrders_thenOnlyOrderOrderTypesManagedByTheUserAreReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        mockOrdersData.addManager(orderManager, existingOrderTypes.get(0));
        mockOrdersData.addManager(orderManager, existingOrderTypes.get(10));

        mockMvcGoGas.loginAs("manager", "password");

        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        List<OrderDTO> expectedOrders = List.of(
                buildExpectedOrderDTO(existingOrderTypes.get(0), "2022-03-20", "2022-03-24", "2022-03-30")
        );

        assertEquals(expectedOrders, availableOrders);
    }

    @Test
    void givenSomeOrdersAlreadyOpened_whenRequestingAequosAvailableOrders_thenOnlyOrderNotOpenedAreReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        mockOrdersData.addManager(orderManager, existingOrderTypes.get(0));
        mockOrdersData.addManager(orderManager, existingOrderTypes.get(1));
        mockOrdersData.addManager(orderManager, existingOrderTypes.get(2));

        mockOrdersData.createOrder(existingOrderTypes.get(0), "2022-03-20", "2022-03-24", "2022-03-30", Order.OrderStatus.Opened);
        mockOrdersData.createOrder(existingOrderTypes.get(1), "2022-03-21", "2022-03-30", "2022-04-10", Order.OrderStatus.Opened);

        mockMvcGoGas.loginAs("manager", "password");

        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        List<OrderDTO> expectedOrders = List.of(
                buildExpectedOrderDTO(existingOrderTypes.get(2), "2022-03-21", "2022-03-26", "2022-04-05")
        );

        assertEquals(expectedOrders, availableOrders);
    }

    @Test
    void givenAllOrdersAlreadyOpened_whenRequestingAequosAvailableOrders_thenNoOrderIsReturned() throws Exception {
        Map<Integer, OrderType> existingOrderTypes = Stream.of(
                mockOrdersData.createAequosOrderType("Fresco Settimanale", 0),
                mockOrdersData.createAequosOrderType("Pane", 1),
                mockOrdersData.createAequosOrderType("Carni", 2),
                mockOrdersData.createAequosOrderType("Mozzarelle", 10)
        ).collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));

        existingOrderTypes.values().forEach(orderType -> mockOrdersData.addManager(orderManager, orderType));

        mockOrdersData.createOrder(existingOrderTypes.get(0), "2022-03-20", "2022-03-24", "2022-03-30", Order.OrderStatus.Opened);
        mockOrdersData.createOrder(existingOrderTypes.get(1), "2022-03-21", "2022-03-30", "2022-04-10", Order.OrderStatus.Opened);
        mockOrdersData.createOrder(existingOrderTypes.get(2), "2022-03-21", "2022-03-26", "2022-04-05", Order.OrderStatus.Opened);

        mockMvcGoGas.loginAs("manager", "password");

        List<OrderDTO> availableOrders = mockMvcGoGas.getDTOList("/api/order/manage/aequos/available", OrderDTO.class);

        assertTrue(availableOrders.isEmpty());
    }

    private AequosOpenOrder buildOpenOrder(int id, String name, String openingDate,
                                           String dueDate, String deliveryDate) {

        AequosOpenOrder aequosOpenOrder = new AequosOpenOrder();
        aequosOpenOrder.setId(id);
        aequosOpenOrder.setDescription(name);
        aequosOpenOrder.setOpeningDate(LocalDate.parse(openingDate));
        aequosOpenOrder.setDueDate(LocalDate.parse(dueDate));
        aequosOpenOrder.setDeliveryDate(LocalDate.parse(deliveryDate));

        return aequosOpenOrder;
    }


    private OrderDTO buildExpectedOrderDTO(OrderType orderType, String openingDate,
                                           String dueDate, String deliveryDate) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderTypeId(orderType.getId().toUpperCase());
        orderDTO.setOrderTypeName(orderType.getDescription());
        orderDTO.setOpeningDate(LocalDate.parse(openingDate));
        orderDTO.setDueDate(LocalDate.parse(dueDate));
        orderDTO.setDeliveryDate(LocalDate.parse(deliveryDate));

        return orderDTO;
    }
}
