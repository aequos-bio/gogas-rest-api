package eu.aequos.gogas.notification.builder;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.OrderItemService;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AccountedNotificationBuilder extends OrderNotificationBuilder {

    private OrderItemService orderItemService;
    private AccountingService accountingService;

    public AccountedNotificationBuilder(OrderItemService orderItemService, AccountingService accountingService) {
        this.orderItemService = orderItemService;
        this.accountingService = accountingService;
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
        String template = "L'ordine *%s* consegnato il *%s* ti è stato addebitato.\n\uD83D\uDCB0 Controlla il tuo saldo! \uD83D\uDCB0";

        String orderType = order.getOrderType().getDescription();
        String formattedDeliveryDate = ConfigurationService.formatDate(order.getDeliveryDate());

        return String.format(template, orderType, formattedDeliveryDate);
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
