package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.OrderAlreadyExistsException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.workflow.OrderWorkflowHandler;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderManagerService extends CrudService<Order, String> {

    private OrderRepo orderRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderWorkflowHandler orderWorkflowHandler;
    private UserService userService;

    public OrderManagerService(OrderRepo orderRepo, OrderManagerRepo orderManagerRepo,
                               OrderWorkflowHandler orderWorkflowHandler, UserService userService) {

        super(orderRepo, "order");
        this.orderRepo = orderRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderWorkflowHandler = orderWorkflowHandler;
        this.userService = userService;
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException(type, id));
    }

    public synchronized Order create(OrderDTO dto) throws OrderAlreadyExistsException {
        List<String> duplicateOrders = orderRepo.findByOrderTypeIdAndDueDateAndDeliveryDate(dto.getOrderTypeId(), dto.getDueDate(), dto.getDeliveryDate());

        if (!duplicateOrders.isEmpty())
            throw new OrderAlreadyExistsException();

        return super.create(dto);
    }

    public List<OrderDTO> search(OrderSearchFilter searchFilter, String userId) {

        List<String> managedOrderTypes = orderManagerRepo.findByUser(userId).stream()
                .map(OrderManager::getOrderType)
                .collect(Collectors.toList());

        if (managedOrderTypeNotFound(managedOrderTypes, searchFilter.getOrderType()))
            return new ArrayList<>();

        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select())
                .and(OrderSpecs::managedByUser, managedOrderTypes)
                .and(OrderSpecs::type, searchFilter.getOrderType())
                .and(OrderSpecs::dueDateFrom, searchFilter.getDueDateFrom())
                .and(OrderSpecs::dueDateTo, searchFilter.getDueDateTo())
                .and(OrderSpecs::deliveryDateFrom, searchFilter.getDeliveryDateFrom())
                .and(OrderSpecs::deliveryDateTo, searchFilter.getDeliveryDateTo())
                .and(OrderSpecs::statusIn, searchFilter.getStatus())
                .and(OrderSpecs::paid, searchFilter.getPaid())
                .build();

        List<Order> orderList = orderRepo.findAll(filter);

        if (orderList.isEmpty())
            return new ArrayList<>();

        Map<String, OrderSummary> orderSummaries = orderRepo.findOrderSummary(orderList.stream()
                .map(Order::getId)
                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(OrderSummary::getOrderId, Function.identity()));


        return orderList.stream()
                .map(entry -> new OrderDTO().fromModel(entry, orderSummaries.get(entry.getId()), getActions(entry, User.Role.A)))
                .collect(Collectors.toList());
    }

    public void changeStatus(String userId, String orderId,
                             String actionCode, int roundType) throws ItemNotFoundException, UserNotAuthorizedException, InvalidOrderActionException {

        User user = userService.getRequired(userId);
        Order order = this.getRequiredWithType(orderId);

        orderWorkflowHandler.changeStatus(user, order, actionCode, roundType);
    }

    private boolean managedOrderTypeNotFound(List<String> managedOrderTypes, String filterOrderType) {
        if (managedOrderTypes.isEmpty())
            return true;

        return filterOrderType != null && filterOrderType.length() > 0
                && !managedOrderTypes.stream().anyMatch(o -> o.equalsIgnoreCase(filterOrderType));
    }

    private List<String> getActions(Order order, User.Role userRole) {
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
