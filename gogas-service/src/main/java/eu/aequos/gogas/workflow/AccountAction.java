package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.order.OrderStatus;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class AccountAction extends OrderStatusAction {

    private PushNotificationSender pushNotificationSender;
    private AccountingService accountingService;

    public AccountAction(OrderItemService orderItemService, OrderRepo orderRepo,
                         SupplierOrderItemRepo supplierOrderItemRepo, GoGasOrder order,
                         PushNotificationSender pushNotificationSender,
                         AccountingService accountingService) {

        super(orderItemService, orderRepo, supplierOrderItemRepo, order, OrderStatus.Accounted);
        this.pushNotificationSender = pushNotificationSender;
        this.accountingService = accountingService;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != OrderStatus.Closed)
            return notValid("Invalid order status");
        
        if (accountingService.isYearClosed(order.getDeliveryDate()))
            return notValid("L'ordine non può essere contabilizzato, l'anno contabile è chiuso");
        
        return valid();
    }

    @Override
    protected void processOrder() throws InvalidOrderActionException {
        order.chargeToUsers();
        pushNotificationSender.sendOrderNotification(order, OrderEvent.Accounted);
    }
}
