package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;

import java.util.ArrayList;
import java.util.List;

public class UndoAccountAction extends OrderStatusAction {

    private AccountingRepo accountingRepo;

    public UndoAccountAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                             AccountingRepo accountingRepo) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Closed);
        this.accountingRepo = accountingRepo;
    }

    @Override
    protected boolean isActionValid() {
        return order.getStatus() == Order.OrderStatus.Accounted;
    }

    @Override
    protected void processOrder() {
        if (order.getOrderType().isComputedAmount())
            orderItemRepo.setAccountedByOrderId(order.getId(), false);
        else
            accountingRepo.setConfirmedByOrderId(order.getId(), false);
    }
}
