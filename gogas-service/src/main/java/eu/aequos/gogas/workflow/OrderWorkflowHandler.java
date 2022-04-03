package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ConfigurationService.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderWorkflowHandler {

    private final OrderItemRepo orderItemRepo;
    private final OrderRepo orderRepo;
    private final SupplierOrderItemRepo supplierOrderItemRepo;
    private final AccountingService accountingService;
    private final ProductRepo productRepo;
    private final ConfigurationService configurationService;
    private final PushNotificationSender pushNotificationSender;

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
                return new AccountAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order, pushNotificationSender, accountingService);

            case "tornachiuso":
                return new UndoAccountAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order, accountingService);

            case "cancel":
                return new CancelAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order);

            case "undocancel":
                return new UndoCancelAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order);

            default:
                throw new InvalidOrderActionException(String.format("Invalid action: %s", changeAction));
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
                return List.of("undocancel");

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

        result.add("modifica");

        if (order.isNotYetOpened())
            result.add("elimina");
        else {
            result.add("dettaglio");
            result.add("cancel");
        }

        if (order.isExpired())
            result.add("chiudi");

        return result;
    }
}
