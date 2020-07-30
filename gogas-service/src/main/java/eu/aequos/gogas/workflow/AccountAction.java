package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class AccountAction extends OrderStatusAction {

    private PushNotificationSender pushNotificationSender;
    private AccountingService accountingService;

    public AccountAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                         SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                         PushNotificationSender pushNotificationSender,
                         AccountingService accountingService) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Accounted);
        this.pushNotificationSender = pushNotificationSender;
        this.accountingService = accountingService;
    }

    @Override
    protected ActionValidity isActionValid() {
        if (order.getStatus() != Order.OrderStatus.Closed)
            return notValid("Invalid order status");
        
        if (accountingService.isYearClosed(order.getDeliveryDate()))
            return notValid("L'ordine non può essere contabilizzato, l'anno contabile è chiuso");
        
        return valid();
    }

    @Override
    protected void processOrder() throws InvalidOrderActionException {
        if (order.getOrderType().isComputedAmount())
            orderItemRepo.setAccountedByOrderId(order.getId(), true);
        else
            updateAccountingEntries();

        pushNotificationSender.sendOrderNotification(order, OrderEvent.Accounted);
    }

    private void updateAccountingEntries() throws InvalidOrderActionException {
        long orderAccountingEntriesCount = accountingService.countEntriesByOrderId(order.getId());
        long orderUsers = orderItemRepo.countDistinctUserByOrderAndSummary(order.getId(), true);

        if (orderAccountingEntriesCount < orderUsers) {
            throw new InvalidOrderActionException("Il numero di movimenti inseriti è minore degli utenti ordinanti");
        }

        accountingService.setEntriesConfirmedByOrderId(order.getId(), true);
    }
}
