package eu.aequos.gogas.service;

import eu.aequos.gogas.attachments.AttachmentService;
import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.ByProductOrderItem;
import eu.aequos.gogas.persistence.entity.derived.FriendTotalOrder;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.entity.derived.OrderItemQtyOnly;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderFriendService {

    private OrderItemService orderItemService;
    private OrderManagerService orderManagerService;
    private ProductService productService;
    private UserService userService;
    private AccountingService accountingService;
    private ExcelGenerationService reportService;
    private AttachmentService attachmentService;

    private List<Product> getProducts(boolean showAllProductsOnPriceList, String orderTypeId, Collection<OpenOrderItem> orderItems) {
        if (showAllProductsOnPriceList)
            return productService.getProductsOnPriceList(orderTypeId);

        Set<String> orderedProductIds = orderItems.stream()
                .map(OpenOrderItem::getProduct)
                .collect(Collectors.toSet());

        return productService.getProducts(orderedProductIds);
    }

    /** Friends **/
    public List<UserOrderItemDTO> getOriginalFriendsOrder(String userId, String orderId) {
        Order order = orderManagerService.getRequiredWithType(orderId);

        userService.getRequired(userId); //DO NOT REMOVE, used for validation

        Map<String, OpenOrderItem> userSummaryOrderItemsMap = orderItemService.getUserOrderItems(userId, orderId, true);
        Map<String, FriendTotalOrder> friendsTotalDeliveredQuantityMap = orderItemService.getFriendTotalQuantity(userId, orderId);

        List<Product> products = getProducts(false, order.getOrderType().getId(), userSummaryOrderItemsMap.values());

        return convertToDTOFriend(userSummaryOrderItemsMap, products, friendsTotalDeliveredQuantityMap);
    }

    private List<UserOrderItemDTO> convertToDTOFriend(Map<String, OpenOrderItem> userOrderMap, List<Product> products,
                                                Map<String, FriendTotalOrder> friendsTotalDeliveredQuantityMap) {
        return products.stream()
                .map(product -> new UserOrderItemDTO().fromModel(product, userOrderMap.get(product.getId()), friendsTotalDeliveredQuantityMap.get(product.getId())))
                .collect(Collectors.toList());
    }

    public List<OrderItemByProductDTO> getFriendOrderItemsByProduct(String userId, String orderId, String productId) throws ItemNotFoundException {
        //DO NOT REMOVE, used for validation
        orderManagerService.getRequiredWithType(orderId);
        userService.getRequired(userId);

        List<ByProductOrderItem> orderItems = orderItemService.getFriendItemsByProduct(userId, productId, orderId);
        Map<String, String> userFullNameMap = userService.getUsersFullNameMap(ListConverter.fromList(orderItems)
                .extractIds(ByProductOrderItem::getUser));

        return orderItems.stream()
                .map(o -> new OrderItemByProductDTO().fromModel(o, userFullNameMap.get(o.getUser())))
                .sorted(Comparator.comparing(OrderItemByProductDTO::getUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setFriendAccounted(String userId, String orderId, String productId, boolean accounted) throws GoGasException {
        BigDecimal summaryUserQty = orderItemService.getSummaryUserQuantityByProduct(userId, productId, orderId)
                .map(OrderItemQtyOnly::getDeliveredQuantity)
                .orElse(BigDecimal.ZERO);

        BigDecimal originalFriendsTotalQty = orderItemService.getFriendQuantitiesByProduct(userId, productId, orderId).stream()
                .map(OrderItemQtyOnly::getDeliveredQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (summaryUserQty.compareTo(originalFriendsTotalQty) != 0)
            throw new GoGasException("Impossibile procedere con la contabilizzazione degli amici: la somma delle quantità ripartite non corrisponde alla quantità effettivamente ritirata");

        orderItemService.accountFriendOrder(userId, orderId, productId, accounted);
        accountingService.updateFriendBalancesFromOrderItems(userId, orderId, productId, accounted);
    }

    @Transactional
    public OrderItemByProductDTO updateFriendDeliveredQty(String userId, String orderId, String productId, String itemId, BigDecimal qty) throws GoGasException {
        //Retrieve summary delivered quantity (actual quantity delivered to user and friends)
        BigDecimal summaryDeliveredQty = orderItemService.getSummaryUserQuantityByProduct(userId, productId, orderId)
                .map(OrderItemQtyOnly::getDeliveredQuantity)
                .orElse(BigDecimal.ZERO);

        //Sum up delivered quantity of friends with the new quantity to be changed
        List<OrderItemQtyOnly> originalFriendOrderItems = orderItemService.getFriendQuantitiesByProduct(userId, productId, orderId);
        BigDecimal friendOnlyTotalDeliveredQty = originalFriendOrderItems.stream()
                .filter(o -> !o.getUser().equalsIgnoreCase(userId))
                .map(o -> o.getId().equalsIgnoreCase(itemId) ? qty : o.getDeliveredQuantity())
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //check that remaining quantity for user (friend referral) is positive (to avoid distributing more quantity than the available one)
        BigDecimal userRemainingQty = summaryDeliveredQty.subtract(friendOnlyTotalDeliveredQty);
        if (userRemainingQty.compareTo(BigDecimal.ZERO) < 0)
            throw new GoGasException("La quantità totale dei singoli ordini supera la quantità ritirata");

        Optional<String> friendReferralOrderItemId = originalFriendOrderItems.stream()
                .filter(o -> o.getUser().equalsIgnoreCase(userId))
                .map(OrderItemQtyOnly::getId)
                .findAny();

        friendReferralOrderItemId.ifPresent(s -> orderItemService.updateDeliveredQty(orderId, s, userRemainingQty));

        //after all checks, perform update of delivered quantity
        orderItemService.updateDeliveredQty(orderId, itemId, qty);

        return new OrderItemByProductDTO()
                .itemIdAndQuantity(friendReferralOrderItemId.orElse(""), userRemainingQty);
    }

    @Transactional
    public OrderItemByProductDTO insertFriendOrderItem(String userId, String orderId, OrderItemUpdateRequest orderItem) throws GoGasException {
        //Retrieve summary delivered quantity (actual quantity delivered to user and friends)
        BigDecimal summaryDeliveredQty = orderItemService.getSummaryUserItemByProduct(userId, orderItem.getProductId(), orderId)
                .map(OrderItem::getDeliveredQuantity)
                .orElse(BigDecimal.ZERO);

        //Sum up delivered quantity of friends with the new quantity to be changed
        List<ByProductOrderItem> originalFriendOrderItems = orderItemService.getFriendItemsByProduct(userId, orderItem.getProductId(), orderId);
        BigDecimal friendOnlyTotalDeliveredQty = originalFriendOrderItems.stream()
                .filter(o -> !o.getUser().equalsIgnoreCase(userId))
                .map(ByProductOrderItem::getDeliveredQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //adding new quantity
        friendOnlyTotalDeliveredQty = friendOnlyTotalDeliveredQty.add(orderItem.getQuantity());

        //check that remaining quantity for user (friend referral) is positive (to avoid distributing more quantity than the available one)
        BigDecimal userRemainingQty = summaryDeliveredQty.subtract(friendOnlyTotalDeliveredQty);
        if (userRemainingQty.compareTo(BigDecimal.ZERO) < 0)
            throw new GoGasException("La quantità totale dei singoli ordini supera la quantità ritirata");

        Optional<String> friendReferralOrderItemId = originalFriendOrderItems.stream()
                .filter(o -> o.getUser().equalsIgnoreCase(userId))
                .map(ByProductOrderItem::getId)
                .findAny();

        friendReferralOrderItemId.ifPresent(s -> orderItemService.updateDeliveredQty(orderId, s, userRemainingQty));

        //after all checks, perform update of delivered quantity
        insertOrderItem(orderId, orderItem);

        return new OrderItemByProductDTO()
                .itemIdAndQuantity(friendReferralOrderItemId.orElse(""), userRemainingQty);
    }

    private void insertOrderItem(String orderId, OrderItemUpdateRequest orderItem) throws GoGasException {
        if (orderItem.getQuantity() == null || orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new GoGasException("Inserire una quantità maggiore di zero");

        User user = userService.getRequired(orderItem.getUserId());
        Product product = productService.getRequired(orderItem.getProductId());
        orderItemService.insertItemByFriendReferral(user, product, orderId, orderItem);
    }

    public List<SelectItemDTO> getFriendsNotOrdering(String userId, String orderId, String productId) {
        Set<String> userIdsAlreadyOrdered = orderItemService.getUsersWithNotSummaryOrder(orderId, productId);
        List<SelectItemDTO> friendsList = userService.getFriendsForSelect(userId, false, false);

        return friendsList.stream()
                .filter(u -> !userIdsAlreadyOrdered.contains(u.getId()))
                .collect(Collectors.toList());
    }

    public AttachmentDTO extractExcelReport(String orderId, String userId) {
        Order order = orderManagerService.getRequiredWithType(orderId);
        byte[] excelReportContent = reportService.extractFriendsOrderDetails(order, userId);
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return attachmentService.buildAttachmentDTO(order, excelReportContent, "_amici", contentType);
    }
}
