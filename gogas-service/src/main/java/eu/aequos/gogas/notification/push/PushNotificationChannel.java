package eu.aequos.gogas.notification.push;

import eu.aequos.gogas.notification.NotificationChannel;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.notification.push.client.PushNotificationClient;
import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class PushNotificationChannel implements NotificationChannel {

    private final String serviceKey;
    private final String serviceAppId;
    private final PushNotificationClient pushNotificationClient;
    private final PushTokenRepo pushTokenRepo;

    public PushNotificationChannel(@Value("${notification.push.apikey}") String serviceKey,
                                   @Value("${notification.push.appid}") String serviceAppId,
                                   PushNotificationClient pushNotificationClient,
                                   PushTokenRepo pushTokenRepo) {

        this.serviceKey = serviceKey;
        this.serviceAppId = serviceAppId;
        this.pushNotificationClient = pushNotificationClient;
        this.pushTokenRepo = pushTokenRepo;
    }

    public void sendOrderNotification(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds) {
        PushNotificationRequest request = buildPushNotificationRequest(order, notificationBuilder, targetUserIds);
        String response = pushNotificationClient.sendNotifications("Bearer " + serviceKey, request);
        log.info("Notification send, response: " + response);
    }

    private List<String> extractNotificationTokens(Set<String> targetUserIds) {
        if (targetUserIds.isEmpty())
            return new ArrayList<>();

        return pushTokenRepo.findTokensByUserIdIn(targetUserIds);
    }

    private PushNotificationRequest buildPushNotificationRequest(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setAppId(serviceAppId);
        request.setHeadings(notificationBuilder.getHeading());
        request.setContents(notificationBuilder.getPushMessage(order));
        request.setTargetUsers(targetUserIds);
        request.setOrderId(order.getId());
        request.setAndroidGroup("order_" + notificationBuilder.getEventName());
        request.setAndroidGroupMessage("$[notif_count] " + notificationBuilder.getMultipleNotificationsHeading());

        return request;
    }
}
