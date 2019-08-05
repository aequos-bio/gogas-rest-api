package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.SmallUserOrderItemDTO;
import eu.aequos.gogas.dto.UserOrderItemDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.OrderClosedException;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderUserService {

    private OrderItemRepo orderItemRepo;
    private OrderManagerService orderManagerService;
    private ProductService productService;
    private UserService userService;

    public OrderUserService(OrderItemRepo orderItemRepo, OrderManagerService orderManagerService,
                            ProductService productService, UserService userService) {
        this.orderItemRepo = orderItemRepo;
        this.orderManagerService = orderManagerService;
        this.productService = productService;
        this.userService = userService;
    }

    public List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws ItemNotFoundException {
        Order order = orderManagerService.getRequiredWithType(orderId);
        User user = userService.getRequired(userId);

        boolean isOrderOpen = order.getStatus().isOpen();
        boolean showAllProductsOnPriceList = isOrderOpen && order.isEditable();
        boolean showGroupedOrderItems = showGroupedOrderItems(isOrderOpen, order.getOrderType(), user);

        Map<String, OpenOrderItem> userOrderMap = orderItemRepo.findByUserAndOrderAndSummary(userId, orderId, showGroupedOrderItems, OpenOrderItem.class).stream()
                .collect(Collectors.toMap(OpenOrderItem::getProduct, Function.identity()));

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
        OrderItem orderItem = updateOrDeleteItem(user, product, orderId, orderItemUpdate);

        Optional<ProductTotalOrder> productTotalOrder = orderItemRepo.totalQuantityAndUsersByProductForOpenOrder(orderId, product.getId());

        return new SmallUserOrderItemDTO()
                .fromModel(product, orderItem, productTotalOrder.orElse(null));
    }

    private OrderItem updateOrDeleteItem(User user, Product product, String orderId, OrderItemUpdateRequest orderItemUpdate) {
        Optional<OrderItem> existingOrderItem = orderItemRepo.findByUserAndOrderAndProductAndSummary(user.getId(), orderId, product.getId(), false);

        if (orderItemUpdate.getQuantity() == null || orderItemUpdate.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            existingOrderItem.ifPresent(orderItemRepo::delete);
            return null;
        }

        OrderItem orderItem = existingOrderItem.orElse(newOrderItem(orderId, user, product.getId()));
        orderItem.setOrderedQuantity(orderItemUpdate.getQuantity());
        orderItem.setUm(orderItemUpdate.getUnitOfMeasure());
        orderItem.setPrice(product.getPrice());
        return orderItemRepo.save(orderItem);
    }

    private OrderItem newOrderItem(String orderId, User user, String productId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(orderId);
        orderItem.setUser(user.getId());
        orderItem.setProduct(productId);

        if (user.getRoleEnum().isFriend())
            orderItem.setFriendReferral(user.getFriendReferral().getId());

        return orderItem;
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
            return orderItemRepo.totalQuantityAndUsersByProductForOpenOrder(orderId);

        return new ArrayList<>();
    }

    private List<UserOrderItemDTO> convertToDTO(Map<String, OpenOrderItem> userOrderMap, List<Product> products,
                                                Map<String, ProductTotalOrder> productTotalOrdersMap) {
        return products.stream()
                .map(product -> new UserOrderItemDTO().fromModel(product, userOrderMap.get(product.getId()), productTotalOrdersMap.get(product.getId())))
                .collect(Collectors.toList());
    }
}
