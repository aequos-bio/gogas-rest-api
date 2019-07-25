package eu.aequos.gogas.service;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.UnknownOrderStatusException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.dto.UserOrderItemDTO;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderUserService {

    private OrderItemRepo orderItemRepo;
    private OrderRepo orderRepo;
    private ProductService productService;
    private UserRepo userRepo;

    public OrderUserService(OrderItemRepo orderItemRepo, OrderRepo orderRepo, ProductService productService, UserRepo userRepo) {
        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.productService = productService;
        this.userRepo = userRepo;
    }

    public List<UserOrderItemDTO> getUserOrderItems(String orderId, String userId) throws UnknownOrderStatusException, ItemNotFoundException {
        Order order = orderRepo.findByIdWithType(orderId).orElseThrow(() -> new ItemNotFoundException("Order"));
        User user = userRepo.findById(userId).orElseThrow(() -> new ItemNotFoundException("User"));

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

    private boolean isOrderEditable(Order order) {
        return new Date().before(order.getDeliveryDateAndTime());
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
