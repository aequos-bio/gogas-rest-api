package eu.aequos.gogas.order.schedule;

import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.schedule.OrderNotificationTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//TODO: create integration test
@ExtendWith(MockitoExtension.class)
class OrderNotificationTaskUnitTest {

    private OrderNotificationTask task;
    @Mock private TenantRegistry tenantRegistry;
    @Mock private OrderRepo orderRepo;
    @Mock private NotificationSender notificationSender;

    @BeforeEach
    void setUp() {
        when(tenantRegistry.getAllTenants()).thenReturn(Set.of("aTenant"));

        Instant instant = LocalDateTime.of(2023, 10, 13, 11, 15).toInstant(ZoneOffset.UTC);
        task = new OrderNotificationTask(tenantRegistry, orderRepo, notificationSender, Clock.fixed(instant, ZoneId.of("UTC")));
    }

    @AfterEach
    void tearDown() {}

    @Test
    void givenNoOrdersExpiringOrInDelivery_whenWhenSendOrderNotifications_thenNoNotificationIsSent() {
        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());

        task.sendOrderNotifications();

        verify(notificationSender, never()).sendOrderNotification(any(), any());
    }

    @Test
    void givenAnOrderExpiringWithinAnHour_whenWhenSendOrderNotifications_thenExpirationNotificationIsSent() {
        Order expiringOrder = buildOrderExpiringAt(12);

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(List.of(expiringOrder));
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());

        task.sendOrderNotifications();

        verify(notificationSender).sendOrderNotification(expiringOrder, OrderEvent.Expiration);
    }

    @Test
    void givenAnOrderAlreadyExpired_whenWhenSendOrderNotifications_thenExpirationNotificationIsSent() {
        Order expiringOrder = buildOrderExpiringAt(11);

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(List.of(expiringOrder));
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());

        task.sendOrderNotifications();

        verify(notificationSender).sendOrderNotification(expiringOrder, OrderEvent.Expiration);
    }

    @Test
    void givenAnOrderExpiringWithinMoreThanAnHour_whenWhenSendOrderNotifications_thenExpirationNotificationIsNotSent() {
        Order expiringOrder = buildOrderExpiringAt(18);

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(List.of(expiringOrder));
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());

        task.sendOrderNotifications();

        verify(notificationSender, never()).sendOrderNotification(any(), any());
    }

    @Test
    void givenAnOrderExpiringAndTheOtherNot_whenWhenSendOrderNotifications_thenExpirationNotificationIsSentJustForExpiring() {
        Order notYetExpiringOrder = buildOrderExpiringAt(18);
        Order expiringOrder = buildOrderExpiringAt(12);

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(List.of(expiringOrder, notYetExpiringOrder));
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());

        task.sendOrderNotifications();

        verify(notificationSender).sendOrderNotification(expiringOrder, OrderEvent.Expiration);
        verify(notificationSender, never()).sendOrderNotification(notYetExpiringOrder, OrderEvent.Expiration);
    }

    @Test
    void givenAnOrderInDeliveryAndTimeAfterReference_whenWhenSendOrderNotifications_thenDeliveryNotificationIsSent() {
        Order inDeliveryOrder = buildInDeliveryOrder();

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(List.of(inDeliveryOrder));

        task.sendOrderNotifications();

        verify(notificationSender, never()).sendOrderNotification(any(), eq(OrderEvent.Expiration));
        verify(notificationSender).sendOrderNotification(inDeliveryOrder, OrderEvent.Delivery);
    }

    @Test
    void givenAnOrderInDeliveryAndTimeBeforeReference_whenWhenSendOrderNotifications_thenDeliveryNotificationIsSent() {
        Instant instant = LocalDateTime.of(2023, 10, 13, 9, 30).toInstant(ZoneOffset.UTC);
        task = new OrderNotificationTask(tenantRegistry, orderRepo, notificationSender, Clock.fixed(instant, ZoneId.of("UTC")));

        Order inDeliveryOrder = buildInDeliveryOrder();

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(Collections.emptyList());
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(List.of(inDeliveryOrder));

        task.sendOrderNotifications();

        verify(notificationSender, never()).sendOrderNotification(any(), eq(OrderEvent.Expiration));
        verify(notificationSender, never()).sendOrderNotification(inDeliveryOrder, OrderEvent.Delivery);
    }

    @Test
    void givenBothExpiringOrderAndDeliveryOrder_whenWhenSendOrderNotifications_thenBothNotificationIsSent() {
        Order expiringOrder = buildOrderExpiringAt(12);
        Order inDeliveryOrder = buildInDeliveryOrder();

        when(orderRepo.findByDueDateGreaterThanEqual(any())).thenReturn(List.of(expiringOrder));
        when(orderRepo.findByDeliveryDateGreaterThanEqual(any())).thenReturn(List.of(inDeliveryOrder));

        task.sendOrderNotifications();

        verify(notificationSender).sendOrderNotification(expiringOrder, OrderEvent.Expiration);
        verify(notificationSender).sendOrderNotification(inDeliveryOrder, OrderEvent.Delivery);
    }

    private static Order buildOrderExpiringAt(int expiringHour) {
        Order expiringOrder = new Order();
        expiringOrder.setId(UUID.randomUUID().toString());
        expiringOrder.setDueDate(LocalDate.of(2023, 10, 13));
        expiringOrder.setDueHour(expiringHour);
        return expiringOrder;
    }

    private static Order buildInDeliveryOrder() {
        Order expiringOrder = new Order();
        expiringOrder.setId(UUID.randomUUID().toString());
        expiringOrder.setDeliveryDate(LocalDate.of(2023, 10, 13));
        return expiringOrder;
    }
}