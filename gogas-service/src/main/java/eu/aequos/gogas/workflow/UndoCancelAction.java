package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;

import java.util.ArrayList;
import java.util.List;

public class UndoCancelAction extends OrderStatusAction {

    public UndoCancelAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                            SupplierOrderItemRepo supplierOrderItemRepo, Order order) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Opened);
    }

    @Override
    protected boolean isActionValid() {
        return order.getStatus() == Order.OrderStatus.Cancelled;
    }

    @Override
    protected void processOrder() {
        orderItemRepo.setCancelledByOrderId(order.getId(), false);
    }
}
