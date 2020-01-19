package eu.aequos.gogas.workflow;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class UndoAccountAction extends OrderStatusAction {

    private AccountingService accountingService;

    public UndoAccountAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                             AccountingService accountingService) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Closed);
        this.accountingService = accountingService;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != Order.OrderStatus.Accounted)
            return notValid("Invalid order status");

        if (accountingService.isYearClosed(order.getDeliveryDate()))
            return notValid("L'ordine non può essere riaperto, l'anno contabile è chiuso");

        return valid();
    }

    @Override
    protected void processOrder() {
        if (order.getOrderType().isComputedAmount())
            orderItemRepo.setAccountedByOrderId(order.getId(), false);
        else
            accountingService.setEntriesConfirmedByOrderId(order.getId(), false);
    }
}
