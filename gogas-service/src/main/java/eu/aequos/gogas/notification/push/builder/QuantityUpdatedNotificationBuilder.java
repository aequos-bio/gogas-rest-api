package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class QuantityUpdatedNotificationBuilder extends OrderPushNotificationBuilder {

    @Override
    protected String getEventName() {
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
    public Stream<NotificationPreferencesView> filterPreferences(GoGasOrder order, List<NotificationPreferencesView> preferences) {
        Set<String> usersOrdering = order.getOrderingUsers();

        return preferences.stream()
                .filter(pref -> usersOrdering.contains(pref.getUserId()))
                .filter(NotificationPreferencesView::onOrderUpdatedQuantity);
    }
}
