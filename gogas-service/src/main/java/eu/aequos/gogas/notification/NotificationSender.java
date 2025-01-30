package eu.aequos.gogas.notification;

import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesViewRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationPreferencesViewRepo notificationPreferencesViewRepo;
    private final List<OrderNotificationBuilder> orderNotificationBuilders;
    private final List<NotificationChannel> notificationChannels;
    private final UserNotificationsCache userNotificationsCache;
    private final UserRepo userRepo;

    public void sendOrderNotification(Order order, OrderEvent event) {
        if (userNotificationsCache.isNotificationAlreadySent(order.getId(), event)) {
            return;
        }

        log.info("Sending order notifications for order {} and event {}", order.getId(), event.name());

        orderNotificationBuilders.stream()
                .filter(notificationBuilder -> notificationBuilder.eventSupported(event))
                .forEach(notificationBuilder -> sendNotification(order, notificationBuilder));

        userNotificationsCache.addNotificationSent(order.getId(), event);
    }

    private void sendNotification(Order order, OrderNotificationBuilder notificationBuilder) {
        Set<String> targetUserIds = extractTargetUserIds(order, notificationBuilder);
        if (targetUserIds.isEmpty()) {
            log.info("No target user found for notifications");
            return;
        }

        notificationChannels.forEach(channel -> {
            try {
                channel.sendOrderNotification(order, notificationBuilder, targetUserIds);
            } catch (Exception ex) {
                log.error("Error while sending order notification through channel " + channel.getClass().getSimpleName(), ex);
            }
        });
    }

    private Set<String> extractTargetUserIds(Order order, OrderNotificationBuilder notificationBuilder) {
        String orderTypeId = order.getOrderType().getId();
        List<NotificationPreferencesView> notificationPrefs = notificationPreferencesViewRepo.findByOrderTypeId(orderTypeId);

        Set<String> enabledUsers = userRepo.findByEnabled(true, UserCoreInfo.class).stream()
                .map(UserCoreInfo::getId)
                .collect(Collectors.toSet());

        return notificationBuilder.filterPreferences(order, notificationPrefs)
                .map(NotificationPreferencesView::getUserId)
                .filter(enabledUsers::contains)
                .collect(Collectors.toSet());
    }
}
