package eu.aequos.gogas.notification;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.multitenancy.TenantContext;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 In memory notification cache to avoid sending multiple notification for the same event/order
 */
@Component
public class UserNotificationsCache {

    private final Map<String, Map<NotificationKey, LocalDateTime>> notificationsSentByTenant = new ConcurrentHashMap<>();

    public boolean isNotificationAlreadySent(String orderId, OrderEvent orderEvent) {
        String tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new GoGasException("Tenant id not found"));

        if (!notificationsSentByTenant.containsKey(tenantId)) {
            return false;
        }

        NotificationKey notificationKey = new NotificationKey(orderEvent, orderId);
        return notificationsSentByTenant.get(tenantId)
                .containsKey(notificationKey);
    }

    public void addNotificationSent(String orderId, OrderEvent orderEvent) {
        String tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new GoGasException("Tenant id not found"));

        NotificationKey notificationKey = new NotificationKey(orderEvent, orderId);

        Map<NotificationKey, LocalDateTime> tenantNotifications = notificationsSentByTenant.computeIfAbsent(tenantId, key -> new ConcurrentHashMap<>());
        tenantNotifications.put(notificationKey, LocalDateTime.now());

        cleanOldNotifications(tenantNotifications);
    }

    private void cleanOldNotifications(Map<NotificationKey, LocalDateTime> tenantNotifications) {
        tenantNotifications.keySet().stream()
                .filter(key -> ChronoUnit.HOURS.between(tenantNotifications.get(key), LocalDateTime.now()) > 24)
                .forEach(tenantNotifications::remove);
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class NotificationKey {
        private final OrderEvent event;
        private final String orderId;
    }
}
