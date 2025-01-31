package eu.aequos.gogas.notification.telegram;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.multitenancy.TenantContext;
import eu.aequos.gogas.notification.NotificationChannel;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationClient;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationRequestDTO;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationChannel implements NotificationChannel {
    private final TelegramNotificationClient telegramNotificationClient;

    public void sendOrderNotification(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds) {
        TelegramNotificationRequestDTO requestDTO = TelegramNotificationRequestDTO.builder()
                .userIds(targetUserIds)
                .text(buildTelegramMessageForOrder(order, notificationBuilder))
                .build();

        String tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new GoGasException("Invalid tenant"));

        telegramNotificationClient.sendNotifications(tenantId, requestDTO);
    }

    private String buildTelegramMessageForOrder(Order order, OrderNotificationBuilder notificationBuilder) {
        String eventMessage = notificationBuilder.getTelegramMessage(order);
        String orderLink = "\n\n[Apri l'ordine su Go\\!Gas](https://order.aequos.bio/order/gogas/dl.php?orderId=" + order.getId() + ")";

        return eventMessage + orderLink;
    }
}
