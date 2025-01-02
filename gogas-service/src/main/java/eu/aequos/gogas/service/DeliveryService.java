package eu.aequos.gogas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.OrderByProductDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderItemDTO;
import eu.aequos.gogas.dto.delivery.DeliveryProductDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderManagerService orderManagerService;
    private final OrderItemRepo orderItemRepo;
    private final UserService userService;
    private final ProductService productService;
    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;
    private final ConfigurationService configurationService;

    public DeliveryOrderDTO getOrderForDelivery(String orderId) {
        Order order = orderManagerService.getRequiredWithType(orderId);

        List<UserDTO> users = userService.getFullActiveUsersByRoles(visibleUserRoles(order.getOrderType()));

        //TODO: fare metodo nel service che recupera meno dati
        Map<String, List<DeliveryOrderItemDTO>> deliveryOrderItems = orderItemRepo.findByOrderAndSummary(orderId, true).stream()
                .collect(Collectors.groupingBy(OrderItem::getProduct, mapping(this::toOrderItemDTO, Collectors.toList())));

        //TODO: usare metodo adhoc
        List<DeliveryProductDTO> deliveryProducts = orderManagerService.getOrderDetailByProduct(order).stream()
                .map(product -> toProductDTO(product, deliveryOrderItems.get(product.getProductId())))
                .collect(Collectors.toList());

        DeliveryOrderDTO deliveryOrder = new DeliveryOrderDTO();
        deliveryOrder.setOrderId(orderId);
        deliveryOrder.setOrderType(order.getOrderType().getDescription());
        deliveryOrder.setDeliveryDate(order.getDeliveryDate());
        deliveryOrder.setProducts(deliveryProducts);
        deliveryOrder.setUsers(users);
        deliveryOrder.setSortUsersByName(getUsersSortingByNameFlag());

        return deliveryOrder;
    }

    private Boolean getUsersSortingByNameFlag() {
        if (configurationService.isUserPositionEnabled()) {
            return null;
        }

        return true;
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
        itemDTO.setOriginalDeliveredQty(item.getDeliveredQuantity());
        itemDTO.setFinalDeliveredQty(item.getDeliveredQuantity());
        return itemDTO;
    }

    private Set<String> visibleUserRoles(OrderType orderType) {
        Set<String> roles = new HashSet<>();
        roles.add(User.Role.U.name());

        if (!orderType.isSummaryRequired())
            roles.add(User.Role.S.name());

        return roles;
    }


    public AttachmentDTO getOrderForDeliveryAsAttachment(String orderId) throws JsonProcessingException {
        DeliveryOrderDTO order = getOrderForDelivery(orderId);

        byte[] content = objectMapper.writeValueAsBytes(order);
        String fileName = buildFileName(order);

        return new AttachmentDTO(content, MediaType.APPLICATION_JSON_UTF8_VALUE, fileName);
    }

    private String buildFileName(DeliveryOrderDTO order) {
        String orderType = order.getOrderType().replace(" ", "_");
        String deliveryDate = order.getDeliveryDate().format(FILE_DATE_FORMATTER);
        return String.format("%s-%s.smj", orderType, deliveryDate);
    }

    @Transactional
    public void updateQuantityFromFile(String orderId, byte[] deliveredOrderFileContent) throws GoGasException, IOException {
        DeliveryOrderDTO deliveredOrder = objectMapper.readValue(deliveredOrderFileContent, DeliveryOrderDTO.class);
        updateQuantityFromDelivered(orderId, deliveredOrder, false);
    }

    @Transactional
    public void updateQuantityFromDelivered(String orderId, DeliveryOrderDTO deliveredOrder,
                                            boolean skipEmptyQuantities) throws GoGasException {

        if (deliveredOrder == null)
            throw new GoGasException("Invalid content");

        if (!orderId.equalsIgnoreCase(deliveredOrder.getOrderId()))
            throw new GoGasException("Order id is not valid");

        Order order = orderManagerService.getRequiredWithType(orderId);

        List<OrderItem> itemsCreated = new ArrayList<>();

        Map<String, String> usersReferralMap = deliveredOrder.getUsers().stream()
                .filter(u -> u.getFriendReferralId() != null)
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getFriendReferralId));

        for (DeliveryProductDTO productDTO : deliveredOrder.getProducts()) {
            Product product = productService.getRequired(productDTO.getProductId());

            if (!product.getType().equals(order.getOrderType().getId())) {
                throw new GoGasException("Product id " + productDTO.getProductId() + " is not valid");
            }

            for (DeliveryOrderItemDTO deliveredItem : productDTO.getOrderItems()) {
                Optional<OrderItem> createdOrderItem = updateOrCreateQuantity(orderId, usersReferralMap, productDTO, deliveredItem, skipEmptyQuantities);
                createdOrderItem.ifPresent(itemsCreated::add);
            }
        }

        if (!itemsCreated.isEmpty())
            orderItemRepo.saveAll(itemsCreated);

        notificationSender.sendOrderNotification(order, OrderEvent.QuantityUpdated);
    }

    private Optional<OrderItem> updateOrCreateQuantity(String orderId, Map<String, String> usersReferralMap,
                                                       DeliveryProductDTO deliveredProduct, DeliveryOrderItemDTO deliveredItem,
                                                       boolean skipEmptyQuantities) {

        if (skipEmptyQuantities && deliveredItem.getFinalDeliveredQty() == null) {
            return Optional.empty();
        }

        if (updateQuantity(orderId, deliveredProduct.getProductId(), deliveredItem))
            return Optional.empty();

        String userReferralId = usersReferralMap.get(deliveredItem.getUserId());
        OrderItem createdOrderItem = createOrderItem(orderId, deliveredProduct, deliveredItem, userReferralId);
        return Optional.of(createdOrderItem);
    }

    private boolean updateQuantity(String orderId, String productId, DeliveryOrderItemDTO deliveredItem) {
        BigDecimal deliveredQuantity = Optional.ofNullable(deliveredItem.getFinalDeliveredQty())
                .orElse(BigDecimal.ZERO);

        int updatedRows = orderItemRepo.updateDeliveredQty(orderId, deliveredItem.getUserId(), productId, deliveredQuantity);

        return updatedRows > 0;
    }

    private OrderItem createOrderItem(String orderId, DeliveryProductDTO product,
                                      DeliveryOrderItemDTO deliveredItem, String referralId) {

        User user = userService.getRequired(deliveredItem.getUserId());

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(orderId);
        orderItem.setUser(user.getId());
        orderItem.setProduct(product.getProductId());
        orderItem.setUm(product.getUnitOfMeasure());
        orderItem.setPrice(product.getPrice());
        orderItem.setOrderedQuantity(BigDecimal.ZERO);
        orderItem.setDeliveredQuantity(deliveredItem.getFinalDeliveredQty());
        orderItem.setSummary(true);

        if (referralId != null)
            orderItem.setFriendReferral(referralId);

        return orderItem;
    }
}
