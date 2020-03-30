package eu.aequos.gogas.workflow;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.order.OrderStatus;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.OrderItemService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class UndoCancelAction extends OrderStatusAction {

    public UndoCancelAction(OrderItemService orderItemService, OrderRepo orderRepo,
                            SupplierOrderItemRepo supplierOrderItemRepo, GoGasOrder order) {

        super(orderItemService, orderRepo, supplierOrderItemRepo, order, OrderStatus.Opened);
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != OrderStatus.Cancelled)
            return notValid("Invalid order status");

        return valid();
    }

    @Override
    protected void processOrder() {
        orderItemService.setCancelledByOrderId(order.getId(), false);
    }
}
