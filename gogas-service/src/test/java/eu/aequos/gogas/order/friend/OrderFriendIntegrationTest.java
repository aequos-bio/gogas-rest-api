package eu.aequos.gogas.order.friend;

import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.excel.order.OrderExportRequest;
import eu.aequos.gogas.excel.order.OrderItemExport;
import eu.aequos.gogas.excel.order.SupplierOrderItemExport;
import eu.aequos.gogas.excel.order.UserExport;
import eu.aequos.gogas.order.manager.OrderManagementBaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderFriendIntegrationTest extends OrderManagementBaseIntegrationTest {

    @Test
    void givenAClosedOrderAndFriendOrderWithBoxUM_whenExtractingFriendExcelReport_thenQuantitiesAreCorrect() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        OrderDTO orderDTO = buildValidOrderDTO(orderTypeComputed.getId());
        String orderId = createOrder(orderDTO);

        addUserOrdersWithFriends(orderId);

        mockMvcGoGas.loginAs("manager", "password");
        mockOrdersData.forceSummaryRequired(orderTypeComputed, true);
        LocalDate deliveryDate = closeOrder(orderId);

        when(excelServiceClient.order(any()))
                .thenReturn("Response".getBytes());

        mockMvcGoGas.loginAs("user1", "password");
        MockHttpServletResponse response = mockMvcGoGas.get("/api/order/friend/" + orderId + "/export")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        mockOrdersData.forceSummaryRequired(orderTypeComputed, false);

        String formattedDeliveryDate = deliveryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(response.getContentAsString()).isEqualTo("Response");
        assertThat(response.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"Fresco_Settimanale-" + formattedDeliveryDate + "_amici.xlsx\"");

        ArgumentCaptor<OrderExportRequest> orderExportRequestCaptor = ArgumentCaptor.forClass(OrderExportRequest.class);
        verify(excelServiceClient).order(orderExportRequestCaptor.capture());

        OrderExportRequest orderExportRequest = orderExportRequestCaptor.getValue();

        Set<String> actualUserIds = orderExportRequest.getUsers().stream()
                .map(UserExport::getId)
                .collect(Collectors.toSet());

        assertThat(actualUserIds).isEqualTo(Set.of(userId1, friendId1a, friendId1b));

        Map<String, Map<String, Double>> quantityByUserAndProduct = orderExportRequest.getUserOrder().stream()
                .collect(Collectors.groupingBy(OrderItemExport::getUserId, Collectors.toMap(OrderItemExport::getProductId, item -> item.getQuantity().doubleValue())));

        Map<String, Map<String, Double>> expectedOrders = Map.of(
                userId1, Map.of(
                        productsByCodeComputed.get("MELE1").getId().toUpperCase(), 3.0,
                        productsByCodeComputed.get("PATATE").getId().toUpperCase(), 4.0,
                        productsByCodeComputed.get("BIRRA1").getId().toUpperCase(), 1.0
                ),
                friendId1a, Map.of(
                        productsByCodeComputed.get("BIRRA1").getId().toUpperCase(), 2.0,
                        productsByCodeComputed.get("MELE2").getId().toUpperCase(), 4.0
                ),
                friendId1b, Map.of(
                        productsByCodeComputed.get("MELE1").getId().toUpperCase(), 8.5,
                        productsByCodeComputed.get("PATATE").getId().toUpperCase(), 2.5
                )
        );

        assertThat(quantityByUserAndProduct).isEqualTo(expectedOrders);

        Map<String, Double> actualQuantityByProduct = orderExportRequest.getSupplierOrder().stream()
                .collect(Collectors.toMap(SupplierOrderItemExport::getProductId, item -> item.getQuantity().doubleValue()));

        Map<String, Double> expectedQuantityByProduct = Map.of(
                productsByCodeComputed.get("MELE1").getId().toUpperCase(), 11.5,
                productsByCodeComputed.get("PATATE").getId().toUpperCase(), 6.5,
                productsByCodeComputed.get("MELE2").getId().toUpperCase(), 4.0,
                productsByCodeComputed.get("BIRRA1").getId().toUpperCase(), 3.0
        );

        assertThat(actualQuantityByProduct).isEqualTo(expectedQuantityByProduct);
    }

    private void addUserOrdersWithFriends(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addComputedUserOrder(orderId, userId1, "MELE1", 3.0, "KG");
        addComputedUserOrder(orderId, userId1, "PATATE", 4.0, "KG");
        addComputedUserOrder(orderId, userId1, "BIRRA1", 1.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "BIRRA1", 2.0, "PZ");
        addComputedUserOrder(orderId, friendId1a, "MELE2", 4.0, "KG");
        addComputedUserOrder(orderId, friendId1b, "MELE1", 1.0, "Cassa");
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

    private LocalDate closeOrder(String orderId) throws Exception {
        LocalDate deliveryDate = LocalDate.now().plusDays(7);
        mockOrdersData.forceOrderDates(orderId, LocalDate.now().minusDays(2), LocalDateTime.now().minusHours(1), deliveryDate);
        performAction(orderId, "close");
        return deliveryDate;
    }
}
