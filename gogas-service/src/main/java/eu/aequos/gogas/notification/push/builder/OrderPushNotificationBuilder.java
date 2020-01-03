package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.List;
import java.util.stream.Stream;

public interface OrderPushNotificationBuilder {

    String getHeading();

    String getMultipleNotificationsHeading();

    String getMessageTemplate();

    Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences);
}
