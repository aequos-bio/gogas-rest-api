package eu.aequos.gogas.notification.push.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.OrderItemService;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AccountedNotificationBuilder extends OrderPushNotificationBuilder {

    private OrderItemService orderItemService;
    private AccountingService accountingService;

    public AccountedNotificationBuilder(OrderItemService orderItemService, AccountingService accountingService) {
        this.orderItemService = orderItemService;
        this.accountingService = accountingService;
    }

    @Override
    protected String getEventName() {
        return "accounted";
    }

    @Override
    public String getHeading() {
        return "Contabilizzazione ordine";
    }

    @Override
    public String getMessageTemplate() {
        return "E' stato contabilizzato l'ordine '%s' consegnato il %s";
    }

    @Override
    public String getMultipleNotificationsHeading() {
        return "ordini contabilizzati";
    }

    @Override
    public Stream<NotificationPreferencesView> filterPreferences(Order order, List<NotificationPreferencesView> preferences) {
        Set<String> orderingUsers = getOrderingUsers(order);

        return preferences.stream()
                .filter(pref -> orderingUsers.contains(pref.getUserId()))
                .filter(NotificationPreferencesView::onOrderAccounted);
    }

    //TODO: potrebbe diventare metodo dell'ordine stesso.... (OOP)
    private Set<String> getOrderingUsers(Order order) {
        if (order.getOrderType().isComputedAmount())
            return orderItemService.getUsersWithOrder(order.getId());

        return accountingService.getUsersWithOrder(order.getId());
    }
}
