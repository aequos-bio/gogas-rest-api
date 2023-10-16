package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class DeliveryNotificationBuilder implements OrderNotificationBuilder {

    private final OrderItemService orderItemService;

    @Override
    public boolean eventSupported(OrderEvent orderEvent) {
        return OrderEvent.Delivery.equals(orderEvent);
    }

    @Override
    public String getEventName() {
        return "delivery";
    }

    @Override
    public String getHeading() {
        return "Consegna ordine";
    }

    @Override
    public String getPushTemplate() {
        return "Oggi è in consegna l'ordine '%s' del %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini in consegna";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "L'ordine *%s* è in consegna oggi.\n\uD83D\uDE9A Controlla gli avvisi del referente \uD83D\uDE9A";

        String orderType = order.getOrderType().getDescription();

        return String.format(template, orderType);
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        Set<String> usersOrdering = orderItemService.getUsersWithOrder(order.getId());

        return preferences.stream()
                .filter(pref -> usersOrdering.contains(pref.getUserId()))
                .filter(NotificationPreferencesView::onOrderDelivery);
    }
}
