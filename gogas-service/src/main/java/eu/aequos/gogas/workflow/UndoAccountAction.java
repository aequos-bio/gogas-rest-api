package eu.aequos.gogas.workflow;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.order.OrderStatus;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class UndoAccountAction extends OrderStatusAction {

    private AccountingService accountingService;

    public UndoAccountAction(OrderItemService orderItemService, OrderRepo orderRepo,
                             SupplierOrderItemRepo supplierOrderItemRepo, GoGasOrder order,
                             AccountingService accountingService) {

        super(orderItemService, orderRepo, supplierOrderItemRepo, order, OrderStatus.Closed);
        this.accountingService = accountingService;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != OrderStatus.Accounted)
            return notValid("Invalid order status");

        if (accountingService.isYearClosed(order.getDeliveryDate()))
            return notValid("L'ordine non può essere riaperto, l'anno contabile è chiuso");

        return valid();
    }

    @Override
    protected void processOrder() {
        order.undoChargeToUsers();
    }
}
