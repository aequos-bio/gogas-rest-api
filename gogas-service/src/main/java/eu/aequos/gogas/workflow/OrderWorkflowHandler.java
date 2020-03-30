package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.order.GoGasOrder;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ConfigurationService.RoundingMode;
import eu.aequos.gogas.service.OrderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderWorkflowHandler {

    private OrderItemService orderItemService;
    private OrderRepo orderRepo;
    private SupplierOrderItemRepo supplierOrderItemRepo;
    private AccountingService accountingService;
    private ProductRepo productRepo;
    private ConfigurationService configurationService;
    private PushNotificationSender pushNotificationSender;
    private ShippingCostRepo shippingCostRepo;

    public OrderWorkflowHandler(OrderItemService orderItemService, OrderRepo orderRepo,
                                SupplierOrderItemRepo supplierOrderItemRepo, AccountingService accountingService,
                                ProductRepo productRepo, ConfigurationService configurationService,
                                PushNotificationSender pushNotificationSender, ShippingCostRepo shippingCostRepo) {

        this.orderItemService = orderItemService;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.accountingService = accountingService;
        this.productRepo = productRepo;
        this.configurationService = configurationService;
        this.pushNotificationSender = pushNotificationSender;
        this.shippingCostRepo = shippingCostRepo;
    }

    @Transactional
    public void changeStatus(GoGasOrder order, String changeAction, int roundType) throws InvalidOrderActionException {
        OrderStatusAction statusAction = getAction(changeAction, order, roundType);
        statusAction.performAction();
    }

    private OrderStatusAction getAction(String changeAction, GoGasOrder order, int roundType) {
        switch (changeAction) {
            case "close":
                RoundingMode roundingMode = RoundingMode.getRoundingMode(roundType);
                return new CloseAction(orderItemService, orderRepo, supplierOrderItemRepo, roundingMode, order, productRepo, configurationService);

            case "reopen":
                return new ReopenAction(orderItemService, orderRepo, supplierOrderItemRepo, order, shippingCostRepo);

            case "contabilizza":
                return new AccountAction(orderItemService, orderRepo, supplierOrderItemRepo, order, pushNotificationSender, accountingService);

            case "tornachiuso":
                return new UndoAccountAction(orderItemService, orderRepo, supplierOrderItemRepo, order, accountingService);

            case "cancel":
                return new CancelAction(orderItemService, orderRepo, supplierOrderItemRepo, order);

            case "undocancel":
                return new UndoCancelAction(orderItemService, orderRepo, supplierOrderItemRepo, order);

            default:
                throw new GoGasException("Action not supported");
        }
    }

    public List<String> getAvailableActions(GoGasOrder order, User.Role userRole) {
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

    private List<String> getOpenedActions(GoGasOrder order) {
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
