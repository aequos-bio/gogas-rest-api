package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.telegram.TelegramTemplate;
import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.UserOrderSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AccountedNotificationBuilder implements OrderNotificationBuilder {

    private final UserOrderSummaryService userOrderSummaryService;

    @Override
    public boolean eventSupported(OrderEvent orderEvent) {
        return OrderEvent.Accounted.equals(orderEvent);
    }

    @Override
    public String getEventName() {
        return "accounted";
    }

    @Override
    public String getHeading() {
        return "Contabilizzazione ordine";
    }

    @Override
    public String getPushTemplate() {
        return "E' stato contabilizzato l'ordine '%s' consegnato il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini contabilizzati";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "L'ordine *%s* consegnato il *%s* ti è stato addebitato\\.\n\uD83D\uDCB0 Controlla il tuo saldo\\! \uD83D\uDCB0";

        String orderType = order.getOrderType().getDescription();
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return TelegramTemplate.resolve(template, orderType, formattedDeliveryDate);
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        Set<String> orderingUsers = userOrderSummaryService.getUsersWithOrder(order.getId());

        return preferences.stream()
                .filter(pref -> orderingUsers.contains(pref.getUserId()))
                .filter(NotificationPreferencesView::onOrderAccounted);
    }
}
