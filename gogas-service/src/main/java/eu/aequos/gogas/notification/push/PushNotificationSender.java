package eu.aequos.gogas.notification.push;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.builder.OrderPushNotificationBuilder;
import eu.aequos.gogas.notification.push.builder.PushNotificationBuilderSelector;
import eu.aequos.gogas.notification.push.client.PushNotificationClient;
import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesViewRepo;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PushNotificationSender {

    private String serviceKey;
    private String serviceAppId;

    private PushNotificationClient pushNotificationClient;
    private NotificationPreferencesViewRepo notificationPreferencesViewRepo;
    private PushNotificationBuilderSelector pushNotificationBuilderSelector;
    private PushTokenRepo pushTokenRepo;

    public PushNotificationSender(@Value("${notification.push.apikey}") String serviceKey,
                                  @Value("${notification.push.appid}") String serviceAppId,
                                  PushNotificationClient pushNotificationClient,
                                  NotificationPreferencesViewRepo notificationPreferencesViewRepo,
                                  PushNotificationBuilderSelector pushNotificationBuilderSelector,
                                  PushTokenRepo pushTokenRepo) {

        this.serviceKey = serviceKey;
        this.serviceAppId = serviceAppId;
        this.pushNotificationClient = pushNotificationClient;
        this.notificationPreferencesViewRepo = notificationPreferencesViewRepo;
        this.pushNotificationBuilderSelector = pushNotificationBuilderSelector;
        this.pushTokenRepo = pushTokenRepo;
    }

    public void sendOrderNotification(Order order, OrderEvent event) {
        log.info("Sending order push notifications for event {}", event.name());
        OrderPushNotificationBuilder pushNotificationBuilder = pushNotificationBuilderSelector.select(event);

        List<String> targetTokens = extractNotificationTokens(order, pushNotificationBuilder);
        if (targetTokens.isEmpty()) {
            log.info("No target user found for push notifications");
            return;
        }

        PushNotificationRequest request = pushNotificationBuilder.buildRequest(order, targetTokens, serviceAppId);
        String response = pushNotificationClient.sendNotifications("Bearer " + serviceKey, request);
        log.info("Notification send, response: " + response);
    }

    private List<String> extractNotificationTokens(Order order, OrderPushNotificationBuilder orderPushNotification) {
        String orderTypeId = order.getOrderType().getId();
        List<NotificationPreferencesView> notificationPrefs = notificationPreferencesViewRepo.findByOrderTypeId(orderTypeId);

        Set<String> targetUsers = orderPushNotification.filterPreferences(order, notificationPrefs)
                .map(NotificationPreferencesView::getUserId)
                .collect(Collectors.toSet());

        if (targetUsers.isEmpty())
            return new ArrayList<>();

        return pushTokenRepo.findTokensByUserIdIn(targetUsers);
    }
}
