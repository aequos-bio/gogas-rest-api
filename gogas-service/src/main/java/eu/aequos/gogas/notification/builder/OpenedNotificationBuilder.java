package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;

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
    public String getPushTemplate() {
        return "E' stato aperto l'ordine '%s' in consegna il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini aperti";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "È aperto l'ordine *%s*:\\n \u23F0 chiusura il *%s* alle %s\\n \uD83D\uDE9A consegna il *%s*";

        String orderType = order.getOrderType().getDescription();
        String formattedDueDate = ConfigurationService.formatDate(order.getDueDate());
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return String.format(template, orderType, formattedDueDate, order.getDueHour(), formattedDeliveryDate);
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderOpened);
    }
}
