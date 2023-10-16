package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public interface OrderNotificationBuilder {

    boolean eventSupported(OrderEvent orderEvent);

    String getEventName();

    String getHeading();

    String getMultipleNotificationsHeading();

    Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences);

    String getPushTemplate();

    String getTelegramMessage(Order order);
}
