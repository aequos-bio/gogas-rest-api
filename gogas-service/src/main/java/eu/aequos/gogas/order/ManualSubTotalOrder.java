package eu.aequos.gogas.order;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;

import java.util.Set;

public class ManualSubTotalOrder extends InternalOrder {

    private OrderItemService orderItemService;

    public ManualSubTotalOrder(Order order, OrderRepo orderRepo, OrderItemRepo orderItemRepo,
                               AccountingService accountingService, UserOrderSummaryRepo userOrderSummaryRepo,
                               OrderItemService orderItemService, ShippingCostRepo shippingCostRepo) {

        super(order, orderRepo, orderItemRepo, accountingService, userOrderSummaryRepo, shippingCostRepo);
        this.orderItemService = orderItemService;
    }

    @Override
    public void chargeToUsers() {
        long orderAccountingEntriesCount = accountingService.countEntriesByOrderId(order.getId());
        long orderUsers = orderItemRepo.countDistinctUserByOrder(order.getId());

        if (orderAccountingEntriesCount < orderUsers) {
            throw new InvalidOrderActionException("Il numero di addebiti inseriti è minore degli utenti ordinanti");
        }

        accountingService.setEntriesConfirmedByOrderId(order.getId(), true);
    }

    @Override
    public void undoChargeToUsers() {
        accountingService.setEntriesConfirmedByOrderId(order.getId(), false);
    }

    @Override
    public Set<String> getChargedUsers() {
        return accountingService.getUsersWithOrder(order.getId());
    }

    @Override
    public void recomputeAllUsersTotal() {
        accountingService.recomputeAllUsersTotal(order.getId());
    }

    @Override
    public void clearOrderItems() {
        accountingService.deleteEntriesForOrder(order.getId());
        orderItemService.deleteByOrderAndSummary(order.getId(), true);
        orderItemService.recomputeAllUsersTotal(order.getId());
    }
}
