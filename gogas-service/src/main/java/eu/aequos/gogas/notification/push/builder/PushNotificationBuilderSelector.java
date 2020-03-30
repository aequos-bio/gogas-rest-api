package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.notification.OrderEvent;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationBuilderSelector {

    public OrderPushNotificationBuilder select(OrderEvent orderEvent) {
        switch (orderEvent) {
            case Opened: return new OpenedNotificationBuilder();
            case Expiration: return new ExpirationNotificationBuilder();
            case Delivery: return new DeliveryNotificationBuilder();
            case QuantityUpdated: return new QuantityUpdatedNotificationBuilder();
            case Accounted: return new AccountedNotificationBuilder();

            default: throw new UnsupportedOperationException();
        }
    }
}
