package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.telegram.TelegramTemplate;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class ExpirationNotificationBuilder implements OrderNotificationBuilder {

    @Override
    public boolean eventSupported(OrderEvent orderEvent) {
        return OrderEvent.Expiration.equals(orderEvent);
    }

    @Override
    public String getEventName() {
        return "expiration";
    }

    @Override
    public String getHeading() {
        return "Scadenza ordine";
    }

    @Override
    public String getPushTemplate() {
        return "E' in scadenza l'ordine '%s' in consegna il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini in scadenza";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "L'ordine *%s* in consegna il *%s* scade alle ore *%s*.\n\u23F0 Affrettati\\! \u23F0";

        String orderType = order.getOrderType().getDescription();
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return TelegramTemplate.resolve(template, orderType, formattedDeliveryDate, Integer.toString(order.getDueHour()));
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderExpiration);
    }
}
