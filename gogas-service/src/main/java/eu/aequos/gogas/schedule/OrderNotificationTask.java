package eu.aequos.gogas.schedule;

import eu.aequos.gogas.multitenancy.TenantContext;
import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static eu.aequos.gogas.persistence.specification.OrderSpecs.SortingType.NONE;

@Slf4j
@Component
public class OrderNotificationTask {

    private TenantRegistry tenantRegistry;
    private OrderRepo orderRepo;
    private NotificationSender notificationSender;

    public OrderNotificationTask(TenantRegistry tenantRegistry, OrderRepo orderRepo,
                                 NotificationSender notificationSender) {
        this.tenantRegistry = tenantRegistry;
        this.orderRepo = orderRepo;
        this.notificationSender = notificationSender;
    }

    @Scheduled(cron = "${notification.task.cron}")
    public void sendOrderNotifications() {
        log.info("Notification process started");

        try {
            for (Object tenantId : tenantRegistry.getDataSourceMap().keySet()) {
                TenantContext.setTenantId((String) tenantId);
                sendNotifications(OrderSpecs::dueDateFrom, OrderEvent.Expiration);
                sendNotifications(OrderSpecs::deliveryDateFrom, OrderEvent.Delivery);
            }
        } finally {
            TenantContext.clearTenantId();
        }
    }

    private void sendNotifications(Function<LocalDate, Specification<Order>> orderDateFilter, OrderEvent event) {
        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select(NONE))
                .and(orderDateFilter, LocalDate.now())
                .build();

        List<Order> orderList = orderRepo.findAll(filter);

        for (Order order : orderList) {
            notificationSender.sendOrderNotification(order, event);
        }
    }
}
