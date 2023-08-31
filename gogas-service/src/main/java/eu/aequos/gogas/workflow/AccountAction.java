package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.UserOrderSummaryService;

import java.util.List;

import static eu.aequos.gogas.workflow.ActionValidity.notValid;
import static eu.aequos.gogas.workflow.ActionValidity.valid;

public class AccountAction extends OrderStatusAction {

    private final NotificationSender notificationSender;
    private final AccountingService accountingService;
    private final UserOrderSummaryService userOrderSummaryService;

    public AccountAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                         SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                         NotificationSender notificationSender, AccountingService accountingService,
                         UserOrderSummaryService userOrderSummaryService) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Accounted);
        this.notificationSender = notificationSender;
        this.accountingService = accountingService;
        this.userOrderSummaryService = userOrderSummaryService;
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
        if (order.getOrderType().isComputedAmount()) {
            updateUserOrders();
        }

        updateAccountingEntries();

        notificationSender.sendOrderNotification(order, OrderEvent.Accounted);
    }

    private void updateUserOrders() {
        orderItemRepo.setAccountedByOrderId(order.getId(), true);
        userOrderSummaryService.recomputeAllUsersTotalForComputedOrder(order.getId());
    }

    private void updateAccountingEntries() throws InvalidOrderActionException {
        List<UserOrderSummary> orderSummaries = userOrderSummaryService.findAccountableByOrderId(order.getId());
        long orderUsers = orderItemRepo.countDistinctUserByOrder(order.getId());

        if (orderSummaries.size() < orderUsers) {
            throw new InvalidOrderActionException("Il numero di movimenti inseriti è minore degli utenti ordinanti");
        }

        accountingService.accountOrder(order, orderSummaries);
    }
}
