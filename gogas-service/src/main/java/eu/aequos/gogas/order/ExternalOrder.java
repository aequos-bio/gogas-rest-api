package eu.aequos.gogas.order;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.service.AccountingService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalOrder extends GoGasOrder {

    private UserOrderSummaryRepo userOrderSummaryRepo;

    public ExternalOrder(Order order, OrderRepo orderRepo, AccountingService accountingService,
                         UserOrderSummaryRepo userOrderSummaryRepo, ShippingCostRepo shippingCostRepo) {

        super(order, orderRepo, accountingService, shippingCostRepo);
        this.userOrderSummaryRepo = userOrderSummaryRepo;
    }

    @Override
    public Map<String, OpenOrderItem> getUserOrderItems(User user) {
        //External orders have no order items
        return Collections.emptyMap();
    }

    @Override
    public void chargeToUsers() {
        long orderAccountingEntriesCount = accountingService.countEntriesByOrderId(order.getId());
        if (orderAccountingEntriesCount == 0)
            throw new InvalidOrderActionException("Inserire almeno un addebito per l'ordine");

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
    public Set<String> getOrderingUsers() {
        return Collections.emptySet();
    }

    @Override
    public List<UserOrderSummary> getUserOrderSummary() {
        return userOrderSummaryRepo.findUserOrderSummaryByOrder(order.getId());
    }

    @Override
    public void recomputeAllUsersTotal() {
        accountingService.recomputeAllUsersTotal(order.getId());
    }

    @Override
    public void clearOrderItems() {
        accountingService.deleteEntriesForOrder(order.getId());
        accountingService.recomputeAllUsersTotal(order.getId());
    }
}
