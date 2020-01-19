package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class CancelAction extends OrderStatusAction {

    public CancelAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                        SupplierOrderItemRepo supplierOrderItemRepo, Order order) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Cancelled);
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != Order.OrderStatus.Opened)
            return notValid("Invalid order status");

        return valid();
    }

    @Override
    protected void processOrder() {
        orderItemRepo.setCancelledByOrderId(order.getId(), true);
    }
}
