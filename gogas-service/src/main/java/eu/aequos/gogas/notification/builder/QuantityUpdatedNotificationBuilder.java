package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public class QuantityUpdatedNotificationBuilder extends OrderNotificationBuilder {

    @Override
    public String getEventName() {
        return "updatedquantity";
    }

    @Override
    public String getHeading() {
        return "Quantità aggiornate";
    }

    @Override
    public String getMessageTemplate() {
        return "Sono state aggiornate le quantità per l'ordine '%s' consegnato il %s";
    }

    @Override
    public String getMultipleNotificationsHeading () {
        return "ordini con quantità aggiornate";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderUpdatedQuantity);
    }
}
