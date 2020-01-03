package eu.aequos.gogas.schedule;

import eu.aequos.gogas.notification.OrderEventPushNotification;
import eu.aequos.gogas.notification.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class OrderNotificationTask {

    private OrderRepo orderRepo;
    private PushNotificationSender pushNotificationSender;

    public OrderNotificationTask(OrderRepo orderRepo, PushNotificationSender pushNotificationSender) {
        this.orderRepo = orderRepo;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Scheduled(cron = "${notification.task.cron}")
    public void sendOrderNotifications() {
        log.info("Notification process started");
        sendNotifications(OrderSpecs::dueDateFrom, OrderEventPushNotification.Expiration);
        sendNotifications(OrderSpecs::deliveryDateFrom, OrderEventPushNotification.Delivery);
    }

    private void sendNotifications(Function<Date, Specification<Order>> orderDateFilter, OrderEventPushNotification event) {
        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select())
                .and(orderDateFilter, new Date())
                .build();

        List<Order> orderList = orderRepo.findAll(filter);

        for (Order order : orderList) {
            pushNotificationSender.sendOrderNotification(order, event);
        }
    }
}
