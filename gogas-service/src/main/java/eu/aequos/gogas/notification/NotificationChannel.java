package eu.aequos.gogas.notification;

import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.persistence.entity.Order;

import java.util.Set;

public interface NotificationChannel {
    void sendOrderNotification(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds);
}
