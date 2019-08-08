package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.*;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.ByProductOrderItem;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.workflow.OrderWorkflowHandler;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderManagerService extends CrudService<Order, String> {

    private OrderRepo orderRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderItemService orderItemService;
    private SupplierOrderItemRepo supplierOrderItemRepo;

    private OrderWorkflowHandler orderWorkflowHandler;

    private UserService userService;
    private ProductService productService;

    public OrderManagerService(OrderRepo orderRepo, OrderManagerRepo orderManagerRepo,
                               OrderItemService orderItemService, SupplierOrderItemRepo supplierOrderItemRepo,
                               OrderWorkflowHandler orderWorkflowHandler, UserService userService,
                               ProductService productService) {

        super(orderRepo, "order");
        this.orderRepo = orderRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderItemService = orderItemService;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.orderWorkflowHandler = orderWorkflowHandler;
        this.userService = userService;
        this.productService = productService;
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException(type, id));
    }

    public List<OrderByProductDTO> getOrderDetailByProduct(String orderId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        //TODO: check user permissions
        boolean isOrderOpen = order.getStatus().isOpen();

        List<ProductTotalOrder> productOrderTotal = orderItemService.getTotalQuantityByProduct(orderId, isOrderOpen);
        Map<String, ProductTotalOrder> productTotalOrders = productOrderTotal
                .toMap(ProductTotalOrder::getProduct);

        List<Product> orderedProducts = productService.getProducts(productTotalOrders.keySet());

        Map<String, SupplierOrderItem> supplierOrderItems = supplierOrderItemRepo.findByOrderId(orderId)
                .toMap(SupplierOrderItem::getProductId);

        return orderedProducts.stream()
                .map(p -> new OrderByProductDTO().fromModel(p, productTotalOrders.get(p.getId()), supplierOrderItems.get(p.getId())))
                .collect(Collectors.toList());
    }



    public synchronized Order create(OrderDTO dto) throws OrderAlreadyExistsException {
        //TODO: check user permissions
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
                .map(entry -> new OrderDTO().fromModel(entry, orderSummaries.get(entry.getId()),  orderWorkflowHandler.getAvailableActions(entry, User.Role.A)))
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

    /*** OPERATIONS *****/

    public List<OrderItemByProductDTO> getOrderItemsByProduct(String orderId, String productId) throws ItemNotFoundException {
        Order order = this.getRequiredWithType(orderId);
        //TODO: check user permissions

        List<ByProductOrderItem> orderItems = orderItemService.getItemsByProduct(productId, orderId, !order.getStatus().isOpen());
        Map<String, String> userFullNameMap = userService.getUsersFullNameMap(orderItems.extractIds(ByProductOrderItem::getUser));

        return orderItems.stream()
                .map(o -> new OrderItemByProductDTO().fromModel(o, userFullNameMap.get(o.getUser())))
                .sorted(Comparator.comparing(OrderItemByProductDTO::getUser))
                .collect(Collectors.toList());
    }

    public boolean updateItemDeliveredQty(String orderId, String orderItemId, BigDecimal deliveredQty) {
        //TODO: check user permissions
        return orderItemService.updateDeliveredQty(orderId, orderItemId, deliveredQty);
    }

    public void insertOrderItem(String orderId, OrderItemUpdateRequest orderItem) throws GoGasException {
        if (orderItem.getQuantity() == null || orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new GoGasException("Inserire una quantità maggiore di zero");

        Order order = getRequired(orderId);

        if (order.getStatus() != Order.OrderStatus.Closed)
            throw new OrderClosedException();

        User user = userService.getRequired(orderItem.getUserId());
        Product product = productService.getRequired(orderItem.getProductId());
        orderItemService.insertItemByManager(user, product, orderId, orderItem);
    }

    public List<SelectItemDTO> getUsersNotOrdering(String orderId, String productId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        Set<String> userIds = orderItemService.getUsersWithOrder(orderId, productId);
        Set<String> userRoles = userService.getAllUserRolesAsString(false, !order.getOrderType().isSummaryRequired());

        return userService.getActiveUsersForSelectByBlackListAndRoles(userIds, userRoles);
    }

    public void updateSupplierOrderQty(String orderId, String productId, int boxes) {
        supplierOrderItemRepo.updateBoxesByOrderIdAndProductId(orderId, productId, new BigDecimal(boxes));
    }

    public void replaceOrderItemWithProduct(String orderId, String orderItemId, String productId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);

        SupplierOrderItem supplierOrderItem = supplierOrderItemRepo.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new ItemNotFoundException("SupplierOrder", Arrays.asList(orderId, productId)));

        orderItemService.replaceOrderItemsProduct(orderItemId, order.getOrderType().isSummaryRequired(), productId, supplierOrderItem);
    }

    @Transactional
    public void updateProductPrice(String orderId, String productId, BigDecimal price) throws GoGasException {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new GoGasException("Il prezzo deve essere un valore maggiore di zero");

        supplierOrderItemRepo.updatePriceByOrderIdAndProductId(orderId, productId, price);
        orderItemService.updatePriceByOrderIdAndProductId(orderId, productId, price);
    }

    public void distributeRemainingQuantities(String orderId, String productId) throws ItemNotFoundException {
        SupplierOrderItem supplierOrderItem = supplierOrderItemRepo.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new ItemNotFoundException("SupplierOrder", Arrays.asList(orderId, productId)));

        List<ByProductOrderItem> productOrderItems = orderItemService.getItemsByProduct(productId, orderId, true);

        BigDecimal totalDeliveredQuantity = productOrderItems.stream()
                .map(ByProductOrderItem::getDeliveredQuantity)
                .reduce(BigDecimal::add)
                .get();

        BigDecimal remainingQuantity = supplierOrderItem.getTotalQuantity().subtract(totalDeliveredQuantity);
        BigDecimal remainingQuantityRatio = remainingQuantity.divide(totalDeliveredQuantity, RoundingMode.HALF_UP);

        orderItemService.increaseDeliveredQtyByProduct(orderId, productId, remainingQuantityRatio);
    }
}
