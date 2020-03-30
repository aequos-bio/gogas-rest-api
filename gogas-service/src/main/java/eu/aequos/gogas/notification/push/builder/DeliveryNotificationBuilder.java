package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class DeliveryNotificationBuilder extends OrderPushNotificationBuilder {

    @Override
    protected String getEventName() {
        return "delivery";
    }

    @Override
    public String getHeading() {
        return "Consegna ordine";
    }

    @Override
    public String getMessageTemplate() {
        return "Oggi è in consegna l'ordine '%s' del %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini in consegna";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(GoGasOrder order, List<NotificationPreferencesView> preferences) {
        Set<String> usersOrdering = order.getOrderingUsers();

        return preferences.stream()
                .filter(pref -> usersOrdering.contains(pref.getUserId()))
                .filter(pref -> filter(order, pref));
    }

    private boolean filter(GoGasOrder order, NotificationPreferencesView preference) {
        return preference.onOrderDelivery() &&
                order.isInDelivery(11, preference.getOnDeliveryMinutesBefore());
    }
}
