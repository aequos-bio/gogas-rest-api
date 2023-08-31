package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;

public abstract class OrderStatusAction {

    protected final OrderItemRepo orderItemRepo;
    protected final OrderRepo orderRepo;
    protected final SupplierOrderItemRepo supplierOrderItemRepo;

    protected final Order order;
    private final Order.OrderStatus targetStatus;

    OrderStatusAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                             Order.OrderStatus targetStatus) {

        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.order = order;
        this.targetStatus = targetStatus;
    }

    public void performAction() throws InvalidOrderActionException {
        ActionValidity validity = isActionValid();
        if (!validity.isValid())
            throw new InvalidOrderActionException(validity.getMessage());

        orderRepo.updateOrderStatus(order.getId(), targetStatus.getStatusCode());

        processOrder();
    }

    protected abstract ActionValidity isActionValid();

    protected abstract void processOrder() throws InvalidOrderActionException;
}

