package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;

import java.util.List;
import java.util.stream.Stream;

public abstract class OrderPushNotificationBuilder {

    protected abstract String getEventName();

    protected abstract String getHeading();

    protected abstract String getMultipleNotificationsHeading();

    protected abstract String getMessageTemplate();

    public abstract Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences);

    private String formatOrderMessage(Order order) {
        String formattedDeliveryDate = ConfigurationService.getDateFormat().format(order.getDeliveryDate());
        String messageTemplate = getMessageTemplate();

        return String.format(messageTemplate, order.getOrderType().getDescription(), formattedDeliveryDate);
    }

    public PushNotificationRequest buildRequest(Order order, List<String> targetTokens, String serviceAppId) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setAppId(serviceAppId);
        request.setHeadings(getHeading());
        request.setContents(formatOrderMessage(order));
        request.setTargetTokens(targetTokens);
        request.setOrderId(order.getId());
        request.setAndroidGroup("order_" + getEventName());
        request.setAndroidGroupMessage("$[notif_count] " + getMultipleNotificationsHeading());

        return request;
    }
}
