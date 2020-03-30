package eu.aequos.gogas.order;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;

import java.util.Set;

public class ComputedAmountOrder extends InternalOrder {

    private OrderItemService orderItemService;

    public ComputedAmountOrder(Order order, OrderRepo orderRepo, OrderItemRepo orderItemRepo,
                               AccountingService accountingService, UserOrderSummaryRepo userOrderSummaryRepo,
                               OrderItemService orderItemService, ShippingCostRepo shippingCostRepo) {

        super(order, orderRepo, orderItemRepo, accountingService, userOrderSummaryRepo, shippingCostRepo);
        this.orderItemService = orderItemService;
    }

    @Override
    public void chargeToUsers() {
        setChargeToUser(true);
    }

    @Override
    public void undoChargeToUsers() {
        setChargeToUser(false);
    }

    private void setChargeToUser(boolean charged) {
        orderItemRepo.setAccountedByOrderId(order.getId(), charged);
        accountingService.updateBalancesFromOrderItemsByOrderId(order.getId());
    }

    @Override
    public Set<String> getChargedUsers() {
        //accounted users are the one with order items
        return getOrderingUsers();
    }

    @Override
    public void recomputeAllUsersTotal() {
        orderItemService.recomputeAllUsersTotal(order.getId());
    }

    @Override
    public void clearOrderItems() {
        orderItemService.deleteByOrderAndSummary(order.getId(), true);
        orderItemService.recomputeAllUsersTotal(order.getId());
    }
}
