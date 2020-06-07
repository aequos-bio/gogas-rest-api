package eu.aequos.gogas;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.mvc.MockMvcGoGas;
import eu.aequos.gogas.mvc.OrderUtil;
import eu.aequos.gogas.mvc.TestUsers;
import eu.aequos.gogas.persistence.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserOrderIntegrationTest {

    private static final String PRODUCT_ID = "A6ED8E0D-D4AC-46F7-9E8A-A340F4EC4F0D";

    @Autowired
    private MockMvcGoGas mockMvcGoGas;

    @Autowired
    private OrderUtil orderUtil;

    private String orderId;

    @BeforeEach
    void setUp() {
        orderId = orderUtil.createOrder(OrderUtil.ORDER_FRESCO_SETTIMANALE);
    }

    @AfterEach
    void tearDown() {
        orderUtil.deleteOrder(orderId);
    }

    @Test
    void givenAValidOrder_whenAddingUserOrderItem_itemIsAdded() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(BigDecimal.ONE);
        request.setUnitOfMeasure("KG");
        request.setUserId(TestUsers.USER1_ID);

        mockMvcGoGas.post("/api/order/user/" + orderId + "/item", request)
                .andExpect(status().isOk())
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.tot", is(1.8)))
                .andExpect(jsonPath("$.qta", is(1)))
                .andExpect(jsonPath("$.qtaRitirata", nullValue()));
        //{"umcollo":"Cassa","price":1.80,"weight":8.50,"qta":1,"qtaRitirata":null,"tot":1.80,"colliOrdinati":0,"kgMancanti":7.5000,"kgRimanenti":1.0000}

        OrderSearchFilter orderSearchFilter = new OrderSearchFilter();
        orderSearchFilter.setOrderType(OrderUtil.ORDER_FRESCO_SETTIMANALE);
        orderSearchFilter.setStatus(Arrays.asList(Order.OrderStatus.Opened.getStatusCode()));
        mockMvcGoGas.post("/api/order/user/list", orderSearchFilter)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", equalToIgnoringCase(orderId)))
                .andExpect(jsonPath("$[0].totaleordine", is(1.8)))
                .andExpect(jsonPath("$[0].numarticoli", is(1)));
    }
}
