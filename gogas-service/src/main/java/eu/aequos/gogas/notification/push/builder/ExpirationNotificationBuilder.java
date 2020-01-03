package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public class ExpirationNotificationBuilder implements OrderPushNotificationBuilder {

    @Override
    public String getHeading() {
        return "Scadenza ordine";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "E' in scadenza l'ordine '{0}' in consegna il {1}";
    }

    @Override
    public String getMessageTemplate() {
        return "ordini in scadenza";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(pref -> filter(order, pref));
    }

    private boolean filter(Order order, NotificationPreferencesView preference) {
        return preference.onOrderExpiration() &&
                order.isExpiring(preference.getOnExpirationMinutesBefore());
    }
}
