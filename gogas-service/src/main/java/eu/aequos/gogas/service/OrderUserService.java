package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.OrderClosedException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderUserService {

    private OrderItemService orderItemService;
    private OrderManagerService orderManagerService;
    private ProductService productService;
    private UserService userService;
    private OrderRepo orderRepo;

    public OrderUserService(OrderItemService orderItemService, OrderManagerService orderManagerService,
                            ProductService productService, UserService userService, OrderRepo orderRepo) {
        this.orderItemService = orderItemService;
        this.orderManagerService = orderManagerService;
        this.productService = productService;
        this.userService = userService;
        this.orderRepo = orderRepo;
    }

    public List<OpenOrderDTO> getOpenOrders(String userId) {
        List<Order> openOrders = orderRepo.getOpenOrders();

        if (openOrders.isEmpty())
            return new ArrayList<>();

        Map<String, List<OpenOrderSummary>> openOrderSummaries = orderRepo.findOpenOrderSummary(userId, openOrders.extractIds(Order::getId)).stream()
                .collect(Collectors.groupingBy(OpenOrderSummary::getOrderId));

        return openOrders.stream()
                .map(o -> new OpenOrderDTO().fromModel(o, openOrderSummaries.getOrDefault(o.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    public List<OrderDTO> search(OrderSearchFilter searchFilter, String userId) {

        List<Order> orderList = getFilteredOrders(searchFilter, userId);

        if (orderList.isEmpty())
            return new ArrayList<>();

        Map<String, UserOrderSummary> orderSummaries = orderRepo.findUserOrderSummary(userId, orderList.extractIds(Order::getId)).stream()
                .collect(Collectors.toMap(OrderSummary::getOrderId, Function.identity()));

        return orderList.stream()
                .map(entry -> new OrderDTO().fromModel(entry, orderSummaries.get(entry.getId())))
                .collect(Collectors.toList());
    }

    private List<Order> getFilteredOrders(OrderSearchFilter searchFilter, String userId) {

        if (searchFilter.inDelivery)
            return orderRepo.getInDeliveryOrders(userId);

        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select())
                .and(OrderSpecs::type, searchFilter.getOrderType())
                .and(OrderSpecs::dueDateFrom, searchFilter.getDueDateFrom())
                .and(OrderSpecs::dueDateTo, searchFilter.getDueDateTo())
                .and(OrderSpecs::deliveryDateFrom, searchFilter.getDeliveryDateFrom())
                .and(OrderSpecs::deliveryDateTo, searchFilter.getDeliveryDateTo())
                .and(OrderSpecs::statusIn, searchFilter.getStatus())
                .build();

        return orderRepo.findAll(filter);
    }

    public List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws ItemNotFoundException {
        Order order = orderManagerService.getRequiredWithType(orderId);
        User user = userService.getRequired(userId);

        boolean isOrderOpen = order.getStatus().isOpen();
        boolean showAllProductsOnPriceList = isOrderOpen && order.isEditable();
        boolean showGroupedOrderItems = showGroupedOrderItems(isOrderOpen, order.getOrderType(), user);

        Map<String, OpenOrderItem> userOrderMap = orderItemService.getUserOrderItems(userId, orderId, showGroupedOrderItems);

        Map<String, ProductTotalOrder> productOrderTotalMap = getProductOrderTotal(orderId, isOrderOpen).stream()
                .collect(Collectors.toMap(ProductTotalOrder::getProduct, Function.identity()));

        List<Product> products = getProducts(showAllProductsOnPriceList, order.getOrderType().getId(), userOrderMap.values());

        return convertToDTO(userOrderMap, products, productOrderTotalMap);
    }

    public SmallUserOrderItemDTO updateUserOrder(String orderId, OrderItemUpdateRequest orderItemUpdate) throws ItemNotFoundException, OrderClosedException {
        Order order = orderManagerService.getRequired(orderId);

        if (!order.getStatus().isOpen() || !order.isEditable())
            throw new OrderClosedException();

        User user = userService.getRequired(orderItemUpdate.getUserId());
        Product product = productService.getRequired(orderItemUpdate.getProductId());
        OrderItem orderItem = orderItemService.updateOrDeleteItemByUser(user, product, orderId, orderItemUpdate);

        Optional<ProductTotalOrder> productTotalOrder = orderItemService.getTotalQuantityByProduct(orderId, product.getId());

        return new SmallUserOrderItemDTO()
                .fromModel(product, orderItem, productTotalOrder.orElse(null));
    }

    private boolean showGroupedOrderItems(boolean isOrderOpen, OrderType orderType, User user) {
        boolean useGroupedOrderItems = !isOrderOpen;

        if (user.getRoleEnum().isFriend())
            useGroupedOrderItems &= !orderType.isSummaryRequired();

        return useGroupedOrderItems;
    }

    private List<Product> getProducts(boolean showAllProductsOnPriceList, String orderTypeId, Collection<OpenOrderItem> orderItems) {
        if (showAllProductsOnPriceList)
            return productService.getProductsOnPriceList(orderTypeId);

        Set<String> orderedProductIds = orderItems.stream()
                .map(OpenOrderItem::getProduct)
                .collect(Collectors.toSet());

        return productService.getProducts(orderedProductIds);
    }

    private List<ProductTotalOrder> getProductOrderTotal(String orderId, boolean isOrderOpen) {
        if (isOrderOpen)
            return orderItemService.getTotalQuantityByProduct(orderId, true);

        return new ArrayList<>();
    }

    private List<UserOrderItemDTO> convertToDTO(Map<String, OpenOrderItem> userOrderMap, List<Product> products,
                                                Map<String, ProductTotalOrder> productTotalOrdersMap) {
        return products.stream()
                .map(product -> new UserOrderItemDTO().fromModel(product, userOrderMap.get(product.getId()), productTotalOrdersMap.get(product.getId())))
                .collect(Collectors.toList());
    }
}
