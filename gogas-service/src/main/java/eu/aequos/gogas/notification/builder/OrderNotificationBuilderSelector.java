package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.service.UserOrderSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderNotificationBuilderSelector {

    private final UserOrderSummaryService userOrderSummaryService;

    public OrderNotificationBuilder select(OrderEvent orderEvent) {
        switch (orderEvent) {
            case Opened: return new OpenedNotificationBuilder();
            case Expiration: return new ExpirationNotificationBuilder();
            case Delivery: return new DeliveryNotificationBuilder(userOrderSummaryService);
            case QuantityUpdated: return new QuantityUpdatedNotificationBuilder();
            case Accounted: return new AccountedNotificationBuilder(userOrderSummaryService);

            default: throw new UnsupportedOperationException();
        }
    }
}
