package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;

import java.util.List;
import java.util.stream.Stream;

public abstract class OrderNotificationBuilder {

    public abstract String getEventName();

    public abstract String getHeading();

    public abstract String getMultipleNotificationsHeading();

    protected abstract String getMessageTemplate();

    public abstract Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences);

    public String formatOrderMessage(Order order) {
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());
        String messageTemplate = getMessageTemplate();

        return String.format(messageTemplate, order.getOrderType().getDescription(), formattedDeliveryDate);
    }
}
