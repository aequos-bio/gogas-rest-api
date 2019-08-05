package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import org.springframework.transaction.annotation.Transactional;

public abstract class OrderStatusAction {

    protected OrderItemRepo orderItemRepo;
    protected OrderRepo orderRepo;
    protected SupplierOrderItemRepo supplierOrderItemRepo;

    protected Order order;
    private Order.OrderStatus targetStatus;

    public OrderStatusAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                             Order.OrderStatus targetStatus) {

        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.order = order;
        this.targetStatus = targetStatus;
    }

    public void performAction() throws InvalidOrderActionException {
        if (!isActionValid())
            throw new InvalidOrderActionException(""); //TODO: add message

        orderRepo.updateOrderStatus(order.getId(), targetStatus.getStatusCode());

        processOrder();
    }

    protected abstract boolean isActionValid();

    protected abstract void processOrder();
}

