package eu.aequos.gogas.order;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.persistence.entity.Order;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserOrderIntegrationTest extends BaseGoGasIntegrationTest {

    private static final String PRODUCT_ID = "A6ED8E0D-D4AC-46F7-9E8A-A340F4EC4F0D";

    @Disabled("da sistemare")
    @Test
    void givenAValidOrder_whenAddingUserOrderItem_itemIsAdded() throws Exception {
        Order openedOrder = mockOrders.createOrder("Fresco settimanale");
        mockMvcGoGas.loginAsSimpleUser();

        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(BigDecimal.ONE);
        request.setUnitOfMeasure("KG");
        request.setUserId(mockUsers.getSimpleUserId());

        mockMvcGoGas.post("/api/order/user/" + openedOrder.getId() + "/item", request)
                .andExpect(status().isOk())
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.tot", is(1.8)))
                .andExpect(jsonPath("$.qta", is(1)))
                .andExpect(jsonPath("$.qtaRitirata", nullValue()));

        OrderSearchFilter orderSearchFilter = new OrderSearchFilter();
        orderSearchFilter.setOrderType(openedOrder.getOrderType().getId());
        orderSearchFilter.setStatus(Arrays.asList(Order.OrderStatus.Opened.getStatusCode()));
        mockMvcGoGas.post("/api/order/user/list", orderSearchFilter)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", equalToIgnoringCase(openedOrder.getId())))
                .andExpect(jsonPath("$[0].totaleordine", is(1.8)))
                .andExpect(jsonPath("$[0].numarticoli", is(1)));
    }
}
