package eu.aequos.gogas.notification;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;

import java.util.function.Predicate;

public enum OrderEventPushNotification {
    Opened(
        "Apertura ordine",
        "E' stato aperto l'ordine '{0}' in consegna il {1}",
        "ordini aperti",
        OrderNotificationTarget.All,
        NotificationPreferencesView::isOnOrderOpened
    ),
    Expiration(
        "Scadenza ordine",
        "E' in scadenza l'ordine '{0}' in consegna il {1}",
        "ordini in scadenza",
        OrderNotificationTarget.NotOrderingOnly,
        NotificationPreferencesView::isOnOrderExpiration
    ),
    Delivery(
        "Consegna ordine",
        "Oggi è in consegna l'ordine '{0}' del {1}",
        "ordini in consegna",
        OrderNotificationTarget.OrderingOnly,
        NotificationPreferencesView::isOnOrderDelivery
    ),
    QuantityUpdated(
        "Quantità aggiornate",
        "Sono state aggiornate le quantità per l'ordine '{0}' consegnato il {1}",
        "ordini con quantità aggiornate",
        OrderNotificationTarget.OrderingOnly,
        NotificationPreferencesView::isOnOrderUpdatedQuantity
    ),
    Accounted(
        "Contabilizzazione ordine",
        "E' stato contabilizzato l'ordine '{0}' consegnato il {1}",
        "ordini contabilizzati",
        OrderNotificationTarget.OrderingOnly,
        NotificationPreferencesView::isOnOrderAccounted
    );

    private final String heading;
    private final String multipleNotificationsHeading;
    private final String messageTemplate;
    private final Predicate<NotificationPreferencesView> preferencesFilter;
    private final OrderNotificationTarget target;

    OrderEventPushNotification(String heading, String multipleNotificationsHeading,
                               String messageTemplate, OrderNotificationTarget target,
                               Predicate<NotificationPreferencesView> preferencesFilter) {

        this.heading = heading;
        this.multipleNotificationsHeading = multipleNotificationsHeading;
        this.messageTemplate = messageTemplate;
        this.preferencesFilter = preferencesFilter;
        this.target = target;
    }

    public String getHeading() {
        return heading;
    }

    public String getMultipleNotificationsHeading() {
        return multipleNotificationsHeading;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public Predicate<NotificationPreferencesView> getPreferencesFilter() {
        return preferencesFilter;
    }

    public OrderNotificationTarget getTarget() {
        return target;
    }
}
