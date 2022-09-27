package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public class OpenedNotificationBuilder extends OrderNotificationBuilder {

    @Override
    public String getEventName() {
        return "opened";
    }

    @Override
    public String getHeading() {
        return "Apertura ordine";
    }

    @Override
    public String getMessageTemplate() {
        return "E' stato aperto l'ordine '%s' in consegna il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini aperti";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderOpened);
    }
}
