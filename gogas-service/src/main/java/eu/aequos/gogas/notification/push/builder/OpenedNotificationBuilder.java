package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public class OpenedNotificationBuilder implements OrderPushNotificationBuilder {

    @Override
    public String getHeading() {
        return "Apertura ordine";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "E' stato aperto l'ordine '{0}' in consegna il {1}";
    }

    @Override
    public String getMessageTemplate() {
        return "ordini aperti";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderOpened);
    }
}
