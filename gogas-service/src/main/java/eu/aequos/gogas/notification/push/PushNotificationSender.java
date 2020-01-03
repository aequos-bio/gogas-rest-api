package eu.aequos.gogas.notification.push;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.builder.OrderPushNotificationBuilder;
import eu.aequos.gogas.notification.push.builder.PushNotificationBuilderSelector;
import eu.aequos.gogas.notification.push.client.PushNotificationClient;
import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesRepo;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PushNotificationSender {

    private String serviceKey;
    private String serviceAppId;

    private PushNotificationClient pushNotificationClient;
    private NotificationPreferencesRepo notificationPreferencesRepo;
    private PushNotificationBuilderSelector pushNotificationBuilderSelector;
    private PushTokenRepo pushTokenRepo;

    public PushNotificationSender(@Value("${notification.push.apikey}") String serviceKey,
                                  @Value("${notification.push.appid}") String serviceAppId,
                                  PushNotificationClient pushNotificationClient,
                                  NotificationPreferencesRepo notificationPreferencesRepo,
                                  PushNotificationBuilderSelector pushNotificationBuilderSelector,
                                  PushTokenRepo pushTokenRepo) {

        this.serviceKey = serviceKey;
        this.serviceAppId = serviceAppId;
        this.pushNotificationClient = pushNotificationClient;
        this.notificationPreferencesRepo = notificationPreferencesRepo;
        this.pushNotificationBuilderSelector = pushNotificationBuilderSelector;
        this.pushTokenRepo = pushTokenRepo;
    }

    public void sendOrderNotification(Order order, OrderEvent event) {
        OrderPushNotificationBuilder pushNotificationBuilder = pushNotificationBuilderSelector.select(event);
        List<String> targetTokens = extractNotificationTokens(order, pushNotificationBuilder);
        PushNotificationRequest request = buildRequest(order.getId(), event, pushNotificationBuilder, targetTokens);
        String response = pushNotificationClient.sendNotifications("Bearer " + serviceKey, request);
        log.info("Notification send, response: " + response);
    }

    private List<String> extractNotificationTokens(Order order, OrderPushNotificationBuilder orderPushNotification) {

        String orderTypeId = order.getOrderType().getId();
        List<NotificationPreferencesView> notificationPrefs = notificationPreferencesRepo.findByOrderTypeId(orderTypeId);

        Set<String> targetUsers = orderPushNotification.filterPreferences(order, notificationPrefs)
                .map(NotificationPreferencesView::getUserId)
                .collect(Collectors.toSet());

        return pushTokenRepo.findTokensByUserIdIn(targetUsers);
    }

    private PushNotificationRequest buildRequest(String orderId, OrderEvent event,
                                                 OrderPushNotificationBuilder orderPushNotification, List<String> targetTokens) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setAppId(serviceAppId);
        request.setHeadings(orderPushNotification.getHeading());
        request.setContents(orderPushNotification.getMessageTemplate());
        request.setTargetTokens(targetTokens);
        request.setOrderId(orderId);
        request.setAndroidGroup("order_" + event.name());
        request.setAndroidGroupMessage("$[notif_count] " + orderPushNotification.getMultipleNotificationsHeading());

        return request;
    }

    /*private List<String> users() {
        return Stream.of("1d5a914a-2880-48dd-af1f-644c3663db51",
        "394672a8-e8c0-448b-9bba-b69619c3415d" ,
        "50284528-b96e-4479-be74-50092baac2e2"  ,
        "58b275cd-bbb0-4c64-be99-3dd7e4ae0235",
        "68cbc4b2-cebf-42b1-9ea5-ee037f0efb63",
        "8ace7d97-b0ab-4b6c-8c04-bc02401789b6",
        "c1276cd8-1b44-465d-be53-452941976131",
        "dfb96e8e-ab56-4c73-8cf1-be808714fa02",
        "f0864c0a-51d8-489e-a137-0838fbae602b").collect(Collectors.toList());
    }*/
}
