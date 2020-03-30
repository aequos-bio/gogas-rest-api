package eu.aequos.gogas.order;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.UserOrderSummaryRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;
import org.springframework.stereotype.Component;

@Component
public class GoGasOrderFactory {

    private OrderItemRepo orderItemRepo;
    private OrderRepo orderRepo;
    private AccountingService accountingService;
    private UserOrderSummaryRepo userOrderSummaryRepo;
    private OrderItemService orderItemService;
    private ShippingCostRepo shippingCostRepo;

    public GoGasOrderFactory(OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                             AccountingService accountingService, UserOrderSummaryRepo userOrderSummaryRepo,
                             OrderItemService orderItemService, ShippingCostRepo shippingCostRepo) {

        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.accountingService = accountingService;
        this.userOrderSummaryRepo = userOrderSummaryRepo;
        this.orderItemService = orderItemService;
        this.shippingCostRepo = shippingCostRepo;
    }

    public GoGasOrder initOrder(String orderId) {
        Order order = getRequiredWithType(orderId);
        return initOrder(order);
    }

    public GoGasOrder initOrder(Order order) {
        if (order.getOrderType().isComputedAmount())
            return new ComputedAmountOrder(order, orderRepo, orderItemRepo, accountingService,
                    userOrderSummaryRepo, orderItemService, shippingCostRepo);

        if (order.getOrderType().isExternal())
            return new ExternalOrder(order, orderRepo, accountingService,
                    userOrderSummaryRepo, shippingCostRepo);

        return new ManualSubTotalOrder(order, orderRepo, orderItemRepo, accountingService,
                userOrderSummaryRepo, orderItemService, shippingCostRepo);
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException("order", id));
    }
}
