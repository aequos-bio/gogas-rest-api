package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;

import java.util.List;
import java.util.stream.Stream;

public class ExpirationNotificationBuilder extends OrderNotificationBuilder {

    @Override
    public String getEventName() {
        return "expiration";
    }

    @Override
    public String getHeading() {
        return "Scadenza ordine";
    }

    @Override
    public String getPushTemplate() {
        return "E' in scadenza l'ordine '%s' in consegna il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini in scadenza";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "L'ordine *%s* in consegna il *%s* scade alle ore *%s*.\n\u23F0 Affrettati! \u23F0";

        String orderType = order.getOrderType().getDescription();
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return String.format(template, orderType, formattedDeliveryDate, order.getDueHour());
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
