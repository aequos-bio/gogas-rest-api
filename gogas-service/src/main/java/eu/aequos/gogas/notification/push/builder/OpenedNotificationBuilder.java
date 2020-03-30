package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;

import java.util.List;
import java.util.stream.Stream;

public class OpenedNotificationBuilder extends OrderPushNotificationBuilder {

    @Override
    protected String getEventName() {
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
    public Stream<NotificationPreferencesView> filterPreferences(GoGasOrder order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderOpened);
    }
}
