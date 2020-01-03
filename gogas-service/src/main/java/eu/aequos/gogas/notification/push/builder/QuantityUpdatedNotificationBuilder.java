package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public class QuantityUpdatedNotificationBuilder implements OrderPushNotificationBuilder {

    @Override
    public String getHeading() {
        return "Quantità aggiornate";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "Sono state aggiornate le quantità per l'ordine '{0}' consegnato il {1}";
    }

    @Override
    public String getMessageTemplate() {
        return "ordini con quantità aggiornate";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderUpdatedQuantity);
    }
}
