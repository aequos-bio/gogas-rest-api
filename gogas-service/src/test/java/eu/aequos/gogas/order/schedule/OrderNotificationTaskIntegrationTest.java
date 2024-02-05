package eu.aequos.gogas.order.schedule;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.SmallUserOrderItemDTO;
import eu.aequos.gogas.notification.push.client.PushNotificationRequest;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationRequestDTO;
import eu.aequos.gogas.notification.telegram.client.TelegramNotificationResponseDTO;
import eu.aequos.gogas.order.OrderBaseIntegrationTest;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.schedule.OrderNotificationTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OrderNotificationTaskIntegrationTest extends OrderBaseIntegrationTest {

    @Autowired
    private OrderNotificationTask orderNotificationTask;
    @MockBean
    private Clock clock;

    private List<PushNotificationRequest> pushNotificationRequestsSent;
    private List<TelegramNotificationRequestDTO> telegramNotificationRequestsSent;

    @BeforeEach
    protected void setUp() {
        super.setUp();

        Clock fixedClock = Clock.fixed(LocalDateTime.parse("2023-10-16T11:15:00").toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        when(clock.instant()).thenReturn(fixedClock.instant());

        pushNotificationRequestsSent = new ArrayList<>();
        when(pushNotificationClient.sendNotifications(any(), any())).thenAnswer(call -> {
            PushNotificationRequest request = call.getArgument(1);
            pushNotificationRequestsSent.add(request);
            return "OK";
        });

        telegramNotificationRequestsSent = new ArrayList<>();
        when(telegramNotificationClient.sendNotifications(any(), any())).thenAnswer(call -> {
            TelegramNotificationRequestDTO request = call.getArgument(1);
            telegramNotificationRequestsSent.add(request);

            TelegramNotificationResponseDTO responseDTO = new TelegramNotificationResponseDTO();
            responseDTO.setErrorCount(0);
            responseDTO.setSentCount(1);
            return responseDTO;
        });

        mockUsersData.addPushNotificationToken(userId1, "token1");
        mockUsersData.addPushNotificationToken(userId2, "token2");
        mockUsersData.addPushNotificationToken(userId3, "token3");
    }

    @Test
    void givenAnOrderExpiringWithinAnHour_whenSendOrderNotifications_thenExpirationNotificationIsSentToAllUsers() throws Exception {
        Order expiringOrder = createOpenOrder();
        addUserOrders(expiringOrder.getId());

        mockOrdersData.forceOrderDates(expiringOrder.getId(), LocalDate.of(2023, 10, 10), LocalDateTime.of(2023, 10, 16, 12, 0), LocalDate.of(2023, 10, 26));

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).hasSize(1);
        assertThat(pushNotificationRequestsSent).hasSize(1);

        TelegramNotificationRequestDTO telegramRequest = telegramNotificationRequestsSent.get(0);
        assertThat(telegramRequest.getText()).isEqualToIgnoringCase("L'ordine *Fresco Settimanale* in consegna il *26/10/2023* scade alle ore *12*\\.\n⏰ Affrettati\\! ⏰\n\n[Apri l'ordine su Go\\!Gas](https://order.aequos.bio/order/gogas/dl.php?orderId=" + expiringOrder.getId() + ")");
        assertThat(telegramRequest.getUserIds()).isNotEmpty();

        PushNotificationRequest pushRequest = pushNotificationRequestsSent.get(0);
        assertThat(pushRequest.getUserIds()).isNotEmpty();
        assertThat(pushRequest.getData().getOrderId()).isEqualToIgnoringCase(expiringOrder.getId());
        assertThat(pushRequest.getContents().getItMessage()).isEqualTo("E' in scadenza l'ordine 'Fresco Settimanale' in consegna il 26/10/2023");
    }

    @Test
    void givenAnOrderExpiringWithinAnHourAlreadySent_whenSendOrderNotifications_thenExpirationNotificationIsNotSentTwice() throws Exception {
        Order expiringOrder = createOpenOrder();
        addUserOrders(expiringOrder.getId());

        mockOrdersData.forceOrderDates(expiringOrder.getId(), LocalDate.of(2023, 10, 10), LocalDateTime.of(2023, 10, 16, 12, 0), LocalDate.of(2023, 10, 26));

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).hasSize(1);
        assertThat(pushNotificationRequestsSent).hasSize(1);

        telegramNotificationRequestsSent.clear();
        pushNotificationRequestsSent.clear();

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).isEmpty();
        assertThat(pushNotificationRequestsSent).isEmpty();
    }

    @Test
    void givenAnOrderNotExpiringWithinAnHour_whenSendOrderNotifications_thenExpirationNotificationIsNotSent() throws Exception {
        Order expiringOrder = createOpenOrder();
        addUserOrders(expiringOrder.getId());

        mockOrdersData.forceOrderDates(expiringOrder.getId(), LocalDate.of(2023, 10, 10), LocalDateTime.of(2023, 10, 16, 18, 0), LocalDate.of(2023, 10, 26));

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).isEmpty();
        assertThat(pushNotificationRequestsSent).isEmpty();
    }

    @Test
    void givenAnOrderInDelivery_whenSendOrderNotifications_thenDeliveryNotificationIsSentToUsersWithOrder() throws Exception {
        Order inDeliveryOrder = createOpenOrder();
        addUserOrders(inDeliveryOrder.getId());

        mockOrdersData.forceOrderDates(inDeliveryOrder.getId(), LocalDate.of(2023, 10, 1), LocalDateTime.of(2023, 10, 10, 12, 0), LocalDate.of(2023, 10, 16));
        closeOrder(inDeliveryOrder.getId());

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).hasSize(1);
        assertThat(pushNotificationRequestsSent).hasSize(1);

        TelegramNotificationRequestDTO telegramRequest = telegramNotificationRequestsSent.get(0);
        assertThat(telegramRequest.getText()).isEqualToIgnoringCase("L'ordine *Fresco Settimanale* è in consegna oggi\\.\n🚚 Controlla gli avvisi del referente 🚚\n\n[Apri l'ordine su Go\\!Gas](https://order.aequos.bio/order/gogas/dl.php?orderId=" + inDeliveryOrder.getId() + ")");
        assertThat(telegramRequest.getUserIds()).isEqualTo(Set.of(userId1, userId2));

        PushNotificationRequest pushRequest = pushNotificationRequestsSent.get(0);
        assertThat(pushRequest.getUserIds()).isEqualTo(Set.of(userId1, userId2));
        assertThat(pushRequest.getData().getOrderId()).isEqualToIgnoringCase(inDeliveryOrder.getId());
        assertThat(pushRequest.getContents().getItMessage()).isEqualTo("Oggi è in consegna l'ordine 'Fresco Settimanale' del 16/10/2023");
    }

    @Test
    void givenAnOrderInDeliveryAlreadySent_whenSendOrderNotifications_thenDeliveryNotificationIsNotSentTwice() throws Exception {
        Order inDeliveryOrder = createOpenOrder();
        addUserOrders(inDeliveryOrder.getId());

        mockOrdersData.forceOrderDates(inDeliveryOrder.getId(), LocalDate.of(2023, 10, 1), LocalDateTime.of(2023, 10, 10, 12, 0), LocalDate.of(2023, 10, 16));
        closeOrder(inDeliveryOrder.getId());

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).hasSize(1);
        assertThat(pushNotificationRequestsSent).hasSize(1);

        telegramNotificationRequestsSent.clear();
        pushNotificationRequestsSent.clear();

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).isEmpty();
        assertThat(pushNotificationRequestsSent).isEmpty();
    }

    @Test
    void givenAnOrderInDeliveryButNotInTheRightHour_whenSendOrderNotifications_thenDeliveryNotificationIsNotSent() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.parse("2023-10-16T08:00:00").toInstant(ZoneOffset.UTC));

        Order inDeliveryOrder = createOpenOrder();
        addUserOrders(inDeliveryOrder.getId());

        mockOrdersData.forceOrderDates(inDeliveryOrder.getId(), LocalDate.of(2023, 10, 1), LocalDateTime.of(2023, 10, 10, 12, 0), LocalDate.of(2023, 10, 16));
        closeOrder(inDeliveryOrder.getId());

        orderNotificationTask.sendOrderNotifications();

        assertThat(telegramNotificationRequestsSent).isEmpty();
        assertThat(pushNotificationRequestsSent).isEmpty();
    }

    private Order createOpenOrder() {
        Order openOrder = mockOrdersData.createOpenOrder(orderTypeComputed);
        createdOrderIds.add(openOrder.getId());
        return openOrder;
    }

    private void addUserOrders(String orderId) throws Exception {
        mockMvcGoGas.loginAs("user1", "password");
        addUserOrder(orderId, userId1, 3.0, "KG");

        mockMvcGoGas.loginAs("user2", "password");
        addUserOrder(orderId, userId2, 1.0, "Cassa");
    }

    private void addUserOrder(String orderId, String userId, double qty, String um) throws Exception {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest();
        request.setUserId(userId);
        request.setProductId(productsByCodeComputed.get("MELE1").getId());
        request.setQuantity(BigDecimal.valueOf(qty));
        request.setUnitOfMeasure(um);

        mockMvcGoGas.postDTO("/api/order/user/" + orderId + "/item", request, SmallUserOrderItemDTO.class);
    }

    private void closeOrder(String orderId) throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        BasicResponseDTO responseDTO = mockMvcGoGas.postDTO("/api/order/manage/" + orderId + "/action/close", null, BasicResponseDTO.class);
        assertEquals("OK", responseDTO.getData());
    }
}