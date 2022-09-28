package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.ConfigurationService;

import java.util.List;
import java.util.stream.Stream;

public class QuantityUpdatedNotificationBuilder extends OrderNotificationBuilder {

    @Override
    public String getEventName() {
        return "updatedquantity";
    }

    @Override
    public String getHeading() {
        return "Quantità aggiornate";
    }

    @Override
    public String getPushTemplate() {
        return "Sono state aggiornate le quantità per l'ordine '%s' consegnato il %s";
    }

    @Override
    public String getMultipleNotificationsHeading () {
        return "ordini con quantità aggiornate";
    }

    @Override
    public String getTelegramMessage(Order order) {
        String template = "Sono state le quantità aggiornate per l'ordine *%s* consegnato il *%s*.\n\u2714 Verifica se hai ricevuto tutto! \u2714";

        String orderType = order.getOrderType().getDescription();
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return String.format(template, orderType, formattedDeliveryDate);
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        return preferences.stream()
                .filter(NotificationPreferencesView::onOrderUpdatedQuantity);
    }
}
