package eu.aequos.gogas.schedule;

import eu.aequos.gogas.multitenancy.TenantContext;
import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationTask {

    private static final int EXPIRATION_NOTIFICATION_AHEAD_MINUTES = 60;
    private static final int DELIVERY_NOTIFICATION_REFERENCE_HOUR = 10;

    private final TenantRegistry tenantRegistry;
    private final OrderRepo orderRepo;
    private final NotificationSender notificationSender;
    private final Clock clock;

    @Scheduled(cron = "${notification.task.cron}")
    public void sendOrderNotifications() {
        log.info("Notification process started");

        try {
            for (String tenantId : tenantRegistry.getAllTenants()) {
                TenantContext.setTenantId(tenantId);
                MDC.put("logFileName", tenantId);

                sendNotifications(orderRepo::findByDueDateGreaterThanEqual, this::isExpiring, OrderEvent.Expiration);
                sendNotifications(orderRepo::findByDeliveryDateGreaterThanEqual, this::isInDelivery, OrderEvent.Delivery);
            }
        } finally {
            TenantContext.clearTenantId();
        }
    }

    private boolean isExpiring(Order order) {
        return isDateTimeWithinMinutesFromNow(order.getDueDateAndTime(), EXPIRATION_NOTIFICATION_AHEAD_MINUTES, clock);
    }

    private boolean isInDelivery(Order order) {
        LocalDateTime deliveryDateAndTime = order.getDeliveryDate().atTime(DELIVERY_NOTIFICATION_REFERENCE_HOUR, 0);
        return isDateTimeWithinMinutesFromNow(deliveryDateAndTime, 0, clock);
    }

    private boolean isDateTimeWithinMinutesFromNow(LocalDateTime orderDate, int minutes, Clock clock) {
        return ChronoUnit.MINUTES.between(LocalDateTime.now(clock), orderDate) < minutes;
    }

    private void sendNotifications(Function<LocalDate, List<Order>> orderRetriever, Function<Order, Boolean> orderFilter, OrderEvent event) {
        List<Order> orderList = orderRetriever.apply(LocalDate.now(clock));

        orderList.stream()
                .filter(orderFilter::apply)
                .forEach(order -> notificationSender.sendOrderNotification(order, event));
    }
}
