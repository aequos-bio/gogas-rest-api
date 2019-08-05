package eu.aequos.gogas.workflow;

import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.*;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.ConfigurationService.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowHandler {

    private OrderManagerRepo orderManagerRepo;
    private OrderItemRepo orderItemRepo;
    private OrderRepo orderRepo;
    private SupplierOrderItemRepo supplierOrderItemRepo;
    private AccountingRepo accountingRepo;
    private ProductRepo productRepo;
    private ConfigurationService configurationService;

    public OrderWorkflowHandler(OrderManagerRepo orderManagerRepo, OrderItemRepo orderItemRepo, OrderRepo orderRepo,
                                SupplierOrderItemRepo supplierOrderItemRepo, AccountingRepo accountingRepo,
                                ProductRepo productRepo, ConfigurationService configurationService) {

        this.orderManagerRepo = orderManagerRepo;
        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.accountingRepo = accountingRepo;
        this.productRepo = productRepo;
        this.configurationService = configurationService;
    }

    @Transactional
    public void changeStatus(User user, Order order, String changeAction, int roundType) throws UserNotAuthorizedException, InvalidOrderActionException {

        if (!user.getRoleEnum().isAdmin() && orderManagerRepo.findByUserAndOrderType(user.getId(), order.getOrderType().getId()).isEmpty())
            throw new UserNotAuthorizedException();

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
                return new AccountAction(orderItemRepo, orderRepo, supplierOrderItemRepo, order, accountingRepo);

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
}
