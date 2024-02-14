package eu.aequos.gogas.notification.push;

import eu.aequos.gogas.notification.NotificationChannel;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.notification.push.client.PushNotificationClient;
import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class PushNotificationChannel implements NotificationChannel {

    private final String serviceKey;
    private final String serviceAppId;
    private final PushNotificationClient pushNotificationClient;

    public PushNotificationChannel(@Value("${notification.push.apikey}") String serviceKey,
                                   @Value("${notification.push.appid}") String serviceAppId,
                                   PushNotificationClient pushNotificationClient) {

        this.serviceKey = serviceKey;
        this.serviceAppId = serviceAppId;
        this.pushNotificationClient = pushNotificationClient;
    }

    public void sendOrderNotification(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds) {
        if (targetUserIds.isEmpty()) {
            log.info("No target user found for push notifications");
            return;
        }

        PushNotificationRequest request = buildPushNotificationRequest(order, notificationBuilder, targetUserIds);
        String response = pushNotificationClient.sendNotifications("Bearer " + serviceKey, request);
        log.info("Notification send, response: " + response);
    }

    private PushNotificationRequest buildPushNotificationRequest(Order order, OrderNotificationBuilder notificationBuilder, Set<String> userIds) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setAppId(serviceAppId);
        request.setHeadings(notificationBuilder.getHeading());
        request.setContents(buildMessageBody(order, notificationBuilder));
        request.setUserIds(userIds);
        request.setOrderId(order.getId());
        request.setAndroidGroup("order_" + notificationBuilder.getEventName());
        request.setAndroidGroupMessage("$[notif_count] " + notificationBuilder.getMultipleNotificationsHeading());

        return request;
    }

    private String buildMessageBody(Order order, OrderNotificationBuilder notificationBuilder) {
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());
        String messageTemplate = notificationBuilder.getPushTemplate();

        return String.format(messageTemplate, order.getOrderType().getDescription(), formattedDeliveryDate);
    }
}
