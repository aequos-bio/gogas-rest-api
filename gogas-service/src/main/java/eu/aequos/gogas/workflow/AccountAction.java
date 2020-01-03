package eu.aequos.gogas.workflow;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;

public class AccountAction extends OrderStatusAction {

    private AccountingRepo accountingRepo;
    private PushNotificationSender pushNotificationSender;

    public AccountAction(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                         SupplierOrderItemRepo supplierOrderItemRepo, Order order,
                         AccountingRepo accountingRepo, PushNotificationSender pushNotificationSender) {

        super(orderItemRepo, orderRepo, supplierOrderItemRepo, order, Order.OrderStatus.Accounted);
        this.accountingRepo = accountingRepo;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Override
    protected boolean isActionValid() {
        return order.getStatus() == Order.OrderStatus.Closed;
    }

    @Override
    protected void processOrder() {
        if (order.getOrderType().isComputedAmount())
            orderItemRepo.setAccountedByOrderId(order.getId(), true);
        else
            updateAccountingEntries();

        pushNotificationSender.sendOrderNotification(order, OrderEvent.Accounted);
    }

    private void updateAccountingEntries() {
        long orderAccountingEntriesCount = accountingRepo.countByOrderId(order.getId());
        long orderUsers = orderItemRepo.countDistinctUserByOrder(order.getId());

        if (orderAccountingEntriesCount < orderUsers) {
            //TODO: throw exception
        }

        accountingRepo.setConfirmedByOrderId(order.getId(), true);
    }
}
