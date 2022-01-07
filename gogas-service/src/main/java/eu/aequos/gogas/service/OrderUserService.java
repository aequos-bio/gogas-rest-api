package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.FilterPagination;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.OrderClosedException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.service.OrderItemService.OrderItemUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.aequos.gogas.converter.ListConverter.toMap;
import static eu.aequos.gogas.persistence.entity.Order.OrderStatus.*;
import static eu.aequos.gogas.persistence.specification.OrderSpecs.SortingType.DELIVERY_DATE;

@RequiredArgsConstructor
@Service
public class OrderUserService {

    public static final List<Integer> DEFAULT_ORDER_STATUS_LIST = Stream.of(Opened, Closed, Accounted)
            .map(Order.OrderStatus::getStatusCode)
            .collect(Collectors.toList());

    private final OrderItemService orderItemService;
    private final OrderManagerService orderManagerService;
    private final ProductService productService;
    private final UserService userService;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final ProductCategoryRepo categoryRepo;

    public List<OpenOrderDTO> getOpenOrders(String userId) {
        List<Order> openOrders = orderRepo.getOpenOrders();

        if (openOrders.isEmpty())
            return new ArrayList<>();

        Set<String> orderIds = ListConverter.fromList(openOrders)
                .extractIds(Order::getId);

        Map<String, User> userMap = userRepo.findAll().stream()
                .collect(toMap(User::getId));

        Map<String, List<OpenOrderSummary>> openOrderSummaries = orderRepo.findOpenOrderSummary(userId, orderIds).stream()
                .collect(Collectors.groupingBy(OpenOrderSummary::getOrderId));

        return openOrders.stream()
                .map(o -> new OpenOrderDTO().fromModel(o, openOrderSummaries.getOrDefault(o.getId(), new ArrayList<>()), userMap))
                .collect(Collectors.toList());
    }

    public List<OrderDTO> search(OrderSearchFilter searchFilter, String userId, String userRole) {

        List<Order> orderList = getFilteredOrders(searchFilter, userId);

        if (orderList.isEmpty())
            return new ArrayList<>();

        Set<String> orderIds = ListConverter.fromList(orderList)
                .extractIds(Order::getId);

        Map<String, UserOrderSummary> orderSummaries = fetchUserOrderSummary(userId, userRole, orderIds).stream()
                .collect(Collectors.toMap(OrderSummary::getOrderId, Function.identity()));

        return orderList.stream()
                .map(entry -> new OrderDTO().fromModel(entry, orderSummaries.get(entry.getId())))
                .collect(Collectors.toList());
    }

    private List<UserOrderSummary> fetchUserOrderSummary(String userId, String userRole, Set<String> orderIds) {
        if (userRole.equals(User.Role.S.name()))
            return orderRepo.findFriendOrderSummary(userId, orderIds);

        return orderRepo.findUserOrderSummary(userId, orderIds);
    }

    private List<Order> getFilteredOrders(OrderSearchFilter searchFilter, String userId) {

        if (searchFilter.getInDelivery() != null && searchFilter.getInDelivery())
            return orderRepo.getInDeliveryOrders(userId);

        List<Integer> status = Optional.ofNullable(searchFilter.getStatus())
                .orElse(DEFAULT_ORDER_STATUS_LIST);

        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select(DELIVERY_DATE))
                .and(OrderSpecs::orderedByUser, userId)
                .and(OrderSpecs::type, searchFilter.getOrderType())
                .and(OrderSpecs::dueDateFrom, searchFilter.getDueDateFrom())
                .and(OrderSpecs::dueDateTo, searchFilter.getDueDateTo())
                .and(OrderSpecs::deliveryDateFrom, searchFilter.getDeliveryDateFrom())
                .and(OrderSpecs::deliveryDateTo, searchFilter.getDeliveryDateTo())
                .and(OrderSpecs::statusIn, status)
                .build();

        return orderRepo.findAll(filter, toPageable(searchFilter.getPagination())).getContent();
    }

    private Pageable toPageable(FilterPagination pagination) {
        if (pagination == null)
            return Pageable.unpaged();

        return PageRequest.of(pagination.getPageNumber(), pagination.getPageSize());
    }

    public List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws ItemNotFoundException {
        Order order = orderManagerService.getRequiredWithType(orderId);
        User user = userService.getRequired(userId);

        //TODO: change to have more OOP
        if (order.getOrderType().isExternal())
            return Collections.emptyList();

        boolean isOrderOpen = order.getStatus().isOpen();
        boolean showAllProductsOnPriceList = isOrderOpen && order.isEditable();
        boolean showGroupedOrderItems = showGroupedOrderItems(isOrderOpen, order.getOrderType(), user);

        Map<String, OpenOrderItem> userOrderMap = orderItemService.getUserOrderItems(userId, orderId, showGroupedOrderItems);

        Map<String, ProductTotalOrder> productOrderTotalMap = getProductOrderTotal(orderId, isOrderOpen).stream()
                .collect(toMap(ProductTotalOrder::getProduct));

        List<Product> products = getProducts(showAllProductsOnPriceList, order.getOrderType().getId(), userOrderMap.values());

        return convertToDTO(userOrderMap, products, productOrderTotalMap);
    }

    public SmallUserOrderItemDTO updateUserOrder(String orderId, OrderItemUpdateRequest updateRequest) throws ItemNotFoundException, OrderClosedException {
        Order order = orderManagerService.getRequired(orderId);

        if (!order.getStatus().isOpen() || !order.isEditable())
            throw new OrderClosedException();

        User user = userService.getRequired(updateRequest.getUserId());
        Product product = productService.getRequired(updateRequest.getProductId());
        OrderItemUpdate orderItemUpdate = orderItemService.updateOrDeleteItemByUser(user, product, orderId, updateRequest);

        Optional<ProductTotalOrder> productTotalOrder = orderItemService.getTotalQuantityByProduct(orderId, product.getId());

        return new SmallUserOrderItemDTO()
                .fromModel(product, orderItemUpdate.getOrderItem(), orderItemUpdate.getItemsAdded(), productTotalOrder.orElse(null));
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

        if (orderedProductIds.isEmpty())
            return Collections.emptyList();

        return productService.getProducts(orderedProductIds);
    }

    public List<ProductTotalOrder> getTotalQuantityByProduct(String orderId) {
        Order order = orderManagerService.getRequiredWithType(orderId);
        return getProductOrderTotal(order.getId(), order.getStatus().isOpen());
    }

    public Optional<ProductTotalOrder> getTotalQuantityByProduct(String orderId, String productId) {
        Order order = orderManagerService.getRequiredWithType(orderId);

        if (!order.getStatus().isOpen())
            return Optional.empty();

        return orderItemService.getTotalQuantityByProduct(order.getId(), productId);
    }

    private List<ProductTotalOrder> getProductOrderTotal(String orderId, boolean isOrderOpen) {
        if (!isOrderOpen)
            return Collections.emptyList();

        return orderItemService.getTotalQuantityByProduct(orderId, true);
    }

    private List<UserOrderItemDTO> convertToDTO(Map<String, OpenOrderItem> userOrderMap, List<Product> products,
                                                Map<String, ProductTotalOrder> productTotalOrdersMap) {
        return products.stream()
                .map(product -> new UserOrderItemDTO().fromModel(product, userOrderMap.get(product.getId()), productTotalOrdersMap.get(product.getId())))
                .collect(Collectors.toList());
    }

    public UserOrderDetailsDTO getOrderDetails(String userId, String orderId, boolean includeTotalAmount) throws ItemNotFoundException {
        Order order = orderManagerService.getRequiredWithType(orderId);
        UserOrderDetailsDTO userOrderDetails = new UserOrderDetailsDTO().fromModel(order);

        if (includeTotalAmount) {
            Optional<UserOrderSummary> orderSummary = orderRepo.findFriendOrderSummary(userId, Collections.singleton(orderId)).stream()
                    .findFirst();

            userOrderDetails.withTotalAmount(orderSummary);
        }

        return userOrderDetails;
    }

    public List<CategoryDTO> getOrderCategories(String orderId) {
        return categoryRepo.findByOrderId(orderId).stream()
                .map(CategoryDTO::fromModel)
                .collect(Collectors.toList());
    }

    public CategoryDTO getNotOrderedItemsByCategory(String userId, String orderId, String categoryId) {
        ProductCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category", categoryId));

        List<Product> products = orderItemService.getNotOrderedProductsByCategory(userId, orderId, categoryId);

        return CategoryDTO.fromModel(category, products);
    }
}
