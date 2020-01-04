package eu.aequos.gogas.service;

import eu.aequos.gogas.dto.OrderByProductDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderItemDTO;
import eu.aequos.gogas.dto.delivery.DeliveryProductDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class DeliveryService {

    private OrderManagerService orderManagerService;
    private OrderItemRepo orderItemRepo;
    private UserService userService;
    private PushNotificationSender pushNotificationSender;

    public DeliveryService(OrderManagerService orderManagerService, OrderItemRepo orderItemRepo,
                           UserService userService, PushNotificationSender pushNotificationSender) {

        this.orderManagerService = orderManagerService;
        this.orderItemRepo = orderItemRepo;
        this.userService = userService;
        this.pushNotificationSender = pushNotificationSender;
    }

    public DeliveryOrderDTO getOrderForDelivery(String orderId) {
        Order order = orderManagerService.getRequiredWithType(orderId);

        //TODO: usare metodo adhoc
        List<OrderByProductDTO> orderProducts = orderManagerService.getOrderDetailByProduct(order);

        //TODO: fare metodo nel service che recupera meno dati
        Map<String, List<DeliveryOrderItemDTO>> orderItemByProduct = orderItemRepo.findByOrderAndSummary(orderId, true).stream()
                .collect(Collectors.groupingBy(OrderItem::getProduct, Collectors.mapping(this::toOrderItemDTO, toList())));

        List<DeliveryProductDTO> deliveryProducts = orderProducts.stream()
                .map(product -> toProductDTO(product, orderItemByProduct.get(product.getProductId())))
                .collect(Collectors.toList());

        List<UserDTO> users = userService.getUsersByRoles(visibleUserRoles(order.getOrderType()));

        DeliveryOrderDTO deliveryOrder = new DeliveryOrderDTO();
        deliveryOrder.setOrderId(orderId);
        deliveryOrder.setProducts(deliveryProducts);
        deliveryOrder.setUsers(users);

        return deliveryOrder;
    }

    private DeliveryProductDTO toProductDTO(OrderByProductDTO product, List<DeliveryOrderItemDTO> orderItems) {
        DeliveryProductDTO deliveryProductDTO = new DeliveryProductDTO();
        deliveryProductDTO.setProductId(product.getProductId());
        deliveryProductDTO.setProductName(product.getProductName());
        deliveryProductDTO.setPrice(product.getPrice());
        deliveryProductDTO.setUnitOfMeasure(product.getUnitOfMeasure());
        deliveryProductDTO.setBoxWeight(product.getBoxWeight());
        deliveryProductDTO.setOrderedBoxes(product.getOrderedBoxes());
        deliveryProductDTO.setOrderItems(orderItems);
        return deliveryProductDTO;
    }

    private DeliveryOrderItemDTO toOrderItemDTO(OrderItem item) {
        DeliveryOrderItemDTO itemDTO = new DeliveryOrderItemDTO();
        itemDTO.setUserId(item.getUser());
        itemDTO.setRequestedQty(item.getOrderedQuantity());
        itemDTO.setDeliveredQty(item.getDeliveredQuantity());
        return itemDTO;
    }

    private Set<String> visibleUserRoles(OrderType orderType) {
        Set<String> roles = new HashSet<>();
        roles.add(User.Role.U.name());

        if (!orderType.isSummaryRequired())
            roles.add(User.Role.S.name());

        return roles;
    }

    @Transactional
    public void updateQuantityFromDelivered(String orderId, DeliveryOrderDTO deliveredOrder) throws GoGasException {
        if (deliveredOrder == null)
            throw new GoGasException("Invalid content");

        if (!orderId.equalsIgnoreCase(deliveredOrder.getOrderId()))
            throw new GoGasException("Order id is not valid");

        Order order = orderManagerService.getRequiredWithType(orderId);

        List<OrderItem> itemsCreated = new ArrayList<>();

        Map<String, String> usersReferralMap = deliveredOrder.getUsers().stream()
                .filter(u -> u.getFriendReferralId() != null)
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getFriendReferralId));

        for (DeliveryProductDTO deliveredProduct : deliveredOrder.getProducts()) {
            for (DeliveryOrderItemDTO deliveredItem : deliveredProduct.getOrderItems()) {
                Optional<OrderItem> createdOrderItem = updateOrCreateQuantity(orderId, usersReferralMap, deliveredProduct, deliveredItem);
                createdOrderItem.ifPresent(itemsCreated::add);
            }
        }

        if (!itemsCreated.isEmpty())
            orderItemRepo.saveAll(itemsCreated);

        pushNotificationSender.sendOrderNotification(order, OrderEvent.QuantityUpdated);
    }

    private Optional<OrderItem> updateOrCreateQuantity(String orderId, Map<String, String> usersReferralMap, DeliveryProductDTO deliveredProduct, DeliveryOrderItemDTO deliveredItem) {
        if (!deliveredItem.isChanged())
            return Optional.empty();

        if (updateQuantity(orderId, deliveredProduct.getProductId(), deliveredItem))
            return Optional.empty();

        String userReferralId = usersReferralMap.get(deliveredItem.getUserId());
        OrderItem createdOrderItem = createOrderItem(orderId, deliveredProduct, deliveredItem, userReferralId);
        return Optional.of(createdOrderItem);
    }

    private boolean updateQuantity(String orderId, String productId, DeliveryOrderItemDTO deliveredItem) {
        BigDecimal deliveredQuantity = Optional.ofNullable(deliveredItem.getDeliveredQty())
                .orElse(BigDecimal.ZERO);

        int updatedRows = orderItemRepo.updateDeliveredQty(orderId, deliveredItem.getUserId(), productId, deliveredQuantity);

        return updatedRows > 0;
    }

    private OrderItem createOrderItem(String orderId, DeliveryProductDTO product,
                                      DeliveryOrderItemDTO deliveredItem, String referralId) {

        //TODO: valutare se leggere i dati del prodotto da DB per evitare inconsistenze

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(orderId);
        orderItem.setUser(deliveredItem.getUserId());
        orderItem.setProduct(product.getProductId());
        orderItem.setUm(product.getUnitOfMeasure());
        orderItem.setPrice(product.getPrice());
        orderItem.setOrderedQuantity(BigDecimal.ZERO);
        orderItem.setDeliveredQuantity(deliveredItem.getDeliveredQty());
        orderItem.setSummary(true);

        if (referralId != null)
            orderItem.setFriendReferral(referralId);

        return orderItem;
    }
}
