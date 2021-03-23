package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class MockOrders implements DisposableBean {

    private final OrderTypeRepo orderTypeRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;

    @WithTenant("integration-test")
    public OrderType createExistingOrderType(String name, Integer aequosId) {
        OrderType orderType = new OrderType();
        orderType.setDescription(name);
        orderType.setAequosOrderId(aequosId);
        orderType.setLastsynchro(LocalDateTime.now());
        return orderTypeRepo.save(orderType);
    }

    @WithTenant("integration-test")
    public Order createExistingOrder(OrderType orderType) {
        Order order = new Order();
        order.setOrderType(orderType);
        order.setStatusCode(Order.OrderStatus.Opened.getStatusCode());
        order.setOpeningDate(LocalDate.now().minusDays(2));
        order.setDueDate(LocalDate.now().plusDays(1));
        order.setDeliveryDate(LocalDate.now().plusDays(2));
        order.setShippingCost(BigDecimal.ZERO);
        return orderRepo.save(order);
    }

    @WithTenant("integration-test")
    public void deleteAllOrderTypes() {
        orderItemRepo.deleteAll();
        orderRepo.deleteAll();
        productRepo.deleteAll();
        orderTypeRepo.deleteAll();
    }

    @Override
    @WithTenant("integration-test")
    public void destroy() throws Exception {
        orderItemRepo.deleteAll();
        orderRepo.deleteAll();
        productRepo.deleteAll();
        orderTypeRepo.deleteAll();
    }
}
