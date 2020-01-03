package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ConfigurationService.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class OrderWorkflowHandler {

    private OrderManagerRepo orderManagerRepo;
    private OrderItemRepo orderItemRepo;
    private OrderRepo orderRepo;
    private SupplierOrderItemRepo supplierOrderItemRepo;
    private AccountingRepo accountingRepo;
    private ProductRepo productRepo;
    private ConfigurationService configurationService;
    private PushNotificationSender pushNotificationSender;

    public OrderWorkflowHandler(OrderManagerRepo orderManagerRepo, OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                                SupplierOrderItemRepo supplierOrderItemRepo, AccountingRepo accountingRepo,
                                ProductRepo productRepo, ConfigurationService configurationService,
                                PushNotificationSender pushNotificationSender) {

        this.orderManagerRepo = orderManagerRepo;
        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.accountingRepo = accountingRepo;
        this.productRepo = productRepo;
        this.configurationService = configurationService;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Transactional
    public void changeStatus(Order order, String changeAction, int roundType) throws InvalidOrderActionException {
        OrderStatusAction statusAction = getAction(changeAction, order, roundType);
        statusAction.performAction();
    }

    private OrderStatusAction getAction(String changeAction, Order order, int roundType) {
        switch (changeAction) {
            case "close":
                RoundingMode roundingMode = RoundingMode.getRoundingMode(roundType);
                return new CloseAction(orderItemRepo, orderRepo, supplierOrderItemRepo, roundingMode, order, productRepo, configurationService);

            case "reopen":
                return new ReopenAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order);

            case "contabilizza":
                return new AccountAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order, accountingRepo, pushNotificationSender);

            case "tornachiuso":
                return new UndoAccountAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order, accountingRepo);

            case "cancel":
                return new CancelAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order);

            case "undocancel":
                return new UndoCancelAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order);

            default:
                return null; //TODO throw exception
        }
    }

    public List<String> getAvailableActions(Order order, User.Role userRole) {
        switch (order.getStatus()) {
            case Opened :
                return getOpenedActions(order);

            case Closed :
                return Arrays.asList("gestisci", "riapri", "contabilizza");

            case Accounted :
                return getAccountedAction(userRole);

            case Cancelled :
                return Arrays.asList("undocancel");

            default:
                return new ArrayList<>();
        }
    }

    private List<String> getAccountedAction(User.Role userRole) {
        List<String> result = new ArrayList<>();
        result.add("dettaglio");

        if (userRole.isAdmin())
            result.add("storna");

        return result;
    }

    private List<String> getOpenedActions(Order order) {
        List<String> result = new ArrayList<>();
        Date now = new Date();

        result.add("modifica");

        if (order.getOpeningDate().after(now))
            result.add("elimina");
        else {
            result.add("dettaglio");
            result.add("cancel");
        }

        if (order.getDueDateAndTime().before(now))
            result.add("chiudi");

        return result;
    }
}
