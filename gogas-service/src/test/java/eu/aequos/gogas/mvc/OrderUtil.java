package eu.aequos.gogas.mvc;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Component
public class OrderUtil {

    public static final String ORDER_FRESCO_SETTIMANALE = "700AF02F-1F67-47C7-838A-5993E2E66FC4";

    private OrderRepo orderRepo;
    private OrderItemRepo orderItemRepo;

    @WithTenant("integration-test")
    public String createOrder(String orderTypeId) {
        OrderType orderType = new OrderType();
        orderType.setId(orderTypeId);

        Order order = new Order();
        order.setOrderType(orderType);
        order.setOpeningDate(LocalDate.now());
        order.setDueDate(LocalDate.now().plusDays(3));
        order.setDueHour(14);
        order.setDeliveryDate(LocalDate.now().plusDays(10));
        order.setStatusCode(Order.OrderStatus.Opened.getStatusCode());
        order.setShippingCost(BigDecimal.ZERO);

        return orderRepo.save(order).getId();
    }

    @WithTenant("integration-test")
    public void deleteOrder(String orderId) {
        orderItemRepo.deleteByOrderAndSummary(orderId, false);
        orderItemRepo.deleteByOrderAndSummary(orderId, true);

        orderRepo.deleteById(orderId);
    }
}
