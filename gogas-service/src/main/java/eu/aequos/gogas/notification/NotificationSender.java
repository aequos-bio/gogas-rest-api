package eu.aequos.gogas.notification;

import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilderSelector;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesViewRepo;
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
    private final OrderNotificationBuilderSelector orderNotificationBuilderSelector;
    private final List<NotificationChannel> notificationChannels;

    public void sendOrderNotification(Order order, OrderEvent event) {
        log.info("Sending order notifications for event {}", event.name());
        OrderNotificationBuilder notificationBuilder = orderNotificationBuilderSelector.select(event);

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

        return notificationBuilder.filterPreferences(order, notificationPrefs)
                .map(NotificationPreferencesView::getUserId)
                .collect(Collectors.toSet());
    }
}
