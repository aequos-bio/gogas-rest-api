package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AccountedNotificationBuilder extends OrderPushNotificationBuilder {

    @Override
    protected String getEventName() {
        return "accounted";
    }

    @Override
    public String getHeading() {
        return "Contabilizzazione ordine";
    }

    @Override
    public String getMessageTemplate() {
        return "E' stato contabilizzato l'ordine '%s' consegnato il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini contabilizzati";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(GoGasOrder order, List<NotificationPreferencesView> preferences) {
        Set<String> orderingUsers = order.getChargedUsers();

        return preferences.stream()
                .filter(pref -> orderingUsers.contains(pref.getUserId()))
                .filter(NotificationPreferencesView::onOrderAccounted);
    }
}
