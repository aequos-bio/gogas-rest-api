package eu.aequos.gogas.order;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.service.AccountingService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public abstract class InternalOrder extends GoGasOrder {

    protected OrderItemRepo orderItemRepo;
    private UserOrderSummaryRepo userOrderSummaryRepo;

    public InternalOrder(Order order, OrderRepo orderRepo, OrderItemRepo orderItemRepo,
                         AccountingService accountingService, UserOrderSummaryRepo userOrderSummaryRepo,
                         ShippingCostRepo shippingCostRepo) {

        super(order, orderRepo, accountingService, shippingCostRepo);
        this.orderItemRepo = orderItemRepo;
        this.userOrderSummaryRepo = userOrderSummaryRepo;
    }

    @Override
    public Map<String, OpenOrderItem> getUserOrderItems(User user) {
        boolean showGroupedOrderItems = showGroupedOrderItems(user);

        return orderItemRepo.findByUserAndOrderAndSummary(user.getId(), order.getId(), showGroupedOrderItems, OpenOrderItem.class).stream()
                .collect(toMap(OpenOrderItem::getProduct, Function.identity()));
    }

    private boolean showGroupedOrderItems(User user) {
        boolean useGroupedOrderItems = !this.isOpen();

        if (user.getRoleEnum().isFriend())
            useGroupedOrderItems &= !this.isSummaryRequired();

        return useGroupedOrderItems;
    }

    @Override
    public Set<String> getOrderingUsers() {
        return orderItemRepo.findUserOrderingBySummary(order.getId(), true);
    }

    @Override
    public List<UserOrderSummary> getUserOrderSummary() {
        Set<String> orderingUsers = getOrderingUsers();
        return userOrderSummaryRepo.findUserOrderSummaryByOrder(order.getId(), orderingUsers);
    }
}
