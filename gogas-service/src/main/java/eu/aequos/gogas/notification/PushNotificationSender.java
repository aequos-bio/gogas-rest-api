package eu.aequos.gogas.notification;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesRepo;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
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
    private PushTokenRepo pushTokenRepo;

    public PushNotificationSender(@Value("${notification.push.apikey}") String serviceKey,
                                  @Value("${notification.push.appid}") String serviceAppId,
                                  PushNotificationClient pushNotificationClient,
                                  NotificationPreferencesRepo notificationPreferencesRepo,
                                  PushTokenRepo pushTokenRepo) {

        this.serviceKey = serviceKey;
        this.serviceAppId = serviceAppId;
        this.pushNotificationClient = pushNotificationClient;
        this.notificationPreferencesRepo = notificationPreferencesRepo;
        this.pushTokenRepo = pushTokenRepo;
    }

    public void sendOrderNotificationToAllUsers(Order order, OrderEventPushNotification event) {
        List<NotificationPreferencesView> notificationPrefs = notificationPreferencesRepo.findByOrderTypeId(order.getOrderType().getId());
        sendNotification(order, event, notificationPrefs);
    }

    public void sendOrderNotificationToSpecificUsers(Order order, OrderEventPushNotification event, Set<String> userIds) {
        List<NotificationPreferencesView> notificationPrefs = notificationPreferencesRepo.findByOrderTypeIdAndUserIdIn(order.getOrderType().getId(), userIds);
        sendNotification(order, event, notificationPrefs);
    }

    private void sendNotification(Order order, OrderEventPushNotification event, List<NotificationPreferencesView> notificationPrefs) {
        List<String> targetTokens = extractNotificationTokens(order, event, notificationPrefs);
        PushNotificationRequest request = buildRequest(order.getId(), event, targetTokens);
        String response = pushNotificationClient.sendNotifications("Bearer " + serviceKey, request);
        log.info("Notification send, response: " + response);
    }

    private List<String> extractNotificationTokens(Order order, OrderEventPushNotification event,
                                                   List<NotificationPreferencesView> notificationPrefs) {

        Set<String> targetUsers = notificationPrefs.stream()
                .filter(event.getPreferencesFilter())
                .filter(pref -> filterByDateAndTime(order, pref, event))
                .map(NotificationPreferencesView::getUserId)
                .collect(Collectors.toSet());

        return pushTokenRepo.findTokensByUserIdIn(targetUsers);
    }

    private boolean filterByDateAndTime(Order order, NotificationPreferencesView preference, OrderEventPushNotification event) {
        switch (event) {
            case Expiration : return order.isExpiring(preference.getOnExpirationMinutesBefore());
            case Delivery : return order.isInDelivery(11, preference.getOnDeliveryMinutesBefore());
            default: return true; //other events are not filtered by date
        }
    }

    public Date addHours(Date originalDate, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }


    private PushNotificationRequest buildRequest(String orderId, OrderEventPushNotification event, List<String> targetTokens) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setAppId(serviceAppId);
        request.setHeadings(event.getHeading());
        request.setContents(event.getMessageTemplate());
        request.setTargetTokens(targetTokens);
        request.setOrderId(orderId);
        request.setAndroidGroup("order_" + event.name());
        request.setAndroidGroupMessage("$[notif_count] " + event.getMultipleNotificationsHeading());

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
