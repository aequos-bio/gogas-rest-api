package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.order.OrderStatus;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.OrderItemService;

public abstract class OrderStatusAction {

    protected OrderItemService orderItemService;
    protected OrderRepo orderRepo;
    protected SupplierOrderItemRepo supplierOrderItemRepo;

    protected GoGasOrder order;
    private OrderStatus targetStatus;

    public OrderStatusAction(OrderItemService orderItemService, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, GoGasOrder order,
                             OrderStatus targetStatus) {

        this.orderItemService = orderItemService;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.order = order;
        this.targetStatus = targetStatus;
    }

    public void performAction() throws InvalidOrderActionException {
        ActionValidity validity = isActionValid();
        if (!validity.isValid())
            throw new InvalidOrderActionException(validity.getMessage());

        order.updateStatus(targetStatus);

        processOrder();
    }

    protected abstract ActionValidity isActionValid();

    protected abstract void processOrder() throws InvalidOrderActionException;
}

