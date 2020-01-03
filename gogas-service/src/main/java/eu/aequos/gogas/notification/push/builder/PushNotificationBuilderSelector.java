package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationBuilderSelector {

    private OrderItemService orderItemService;
    private AccountingService accountingService;

    public PushNotificationBuilderSelector(OrderItemService orderItemService, AccountingService accountingService) {
        this.orderItemService = orderItemService;
        this.accountingService = accountingService;
    }

    public OrderPushNotificationBuilder select(OrderEvent orderEvent) {
        switch (orderEvent) {
            case Opened: return new OpenedNotificationBuilder();
            case Expiration: return new ExpirationNotificationBuilder();
            case Delivery: return new DeliveryNotificationBuilder(orderItemService);
            case QuantityUpdated: return new QuantityUpdatedNotificationBuilder();
            case Accounted: return new AccountedNotificationBuilder(orderItemService, accountingService);

            default: throw new UnsupportedOperationException();
        }
    }
}
