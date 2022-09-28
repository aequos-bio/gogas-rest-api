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

    public abstract Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences);

    protected abstract String getPushTemplate();

    public String getPushMessage(Order order) {
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());
        String messageTemplate = getPushTemplate();

        return String.format(messageTemplate, order.getOrderType().getDescription(), formattedDeliveryDate);
    }

    public abstract String getTelegramMessage(Order order);
}
