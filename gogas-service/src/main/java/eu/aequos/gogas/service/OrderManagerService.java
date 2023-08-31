package eu.aequos.gogas.service;

import eu.aequos.gogas.attachments.AttachmentService;
import eu.aequos.gogas.attachments.AttachmentType;
import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.*;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.integration.api.AequosOpenOrder;
import eu.aequos.gogas.integration.api.OrderSynchItem;
import eu.aequos.gogas.integration.api.OrderSynchResponse;
import eu.aequos.gogas.notification.NotificationSender;
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.workflow.OrderWorkflowHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.aequos.gogas.converter.ListConverter.toMap;
import static eu.aequos.gogas.persistence.entity.Order.OrderStatus.Closed;
import static eu.aequos.gogas.persistence.entity.Order.OrderStatus.Opened;
import static eu.aequos.gogas.persistence.specification.OrderSpecs.SortingType.DELIVERY_DATE;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderManagerService extends CrudService<Order, String> {

    public static final String NOT_AEQUOS_ORDER_ERROR_MESSAGE = "Order type is not linked to Aequos";
    private final OrderRepo orderRepo;
    private final OrderManagerRepo orderManagerRepo;
    private final OrderItemService orderItemService;
    private final SupplierOrderItemRepo supplierOrderItemRepo;
    private final ShippingCostRepo shippingCostRepo;

    private final OrderWorkflowHandler orderWorkflowHandler;

    private final UserService userService;
    private final OrderTypeService orderTypeService;
    private final ProductService productService;
    private final AequosIntegrationService aequosIntegrationService;
    private final NotificationSender notificationSender;
    private final AttachmentService attachmentService;
    private final ExcelGenerationService reportService;
    private final OrderPlanningService orderPlanningService;
    private final UserOrderSummaryService userOrderSummaryService;

    @Override
    protected CrudRepository<Order, String> getCrudRepository() {
        return orderRepo;
    }

    @Override
    protected String getType() {
        return "order";
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException(getType(), id));
    }

    public List<OrderByProductDTO> getOrderDetailByProduct(String orderId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        return getOrderDetailByProduct(order);
    }

    public List<OrderByProductDTO> getOrderDetailByProduct(Order order) throws ItemNotFoundException {
        boolean isOrderOpen = order.getStatus().isOpen();

        List<ProductTotalOrder> productOrderTotal = orderItemService.getTotalQuantityByProduct(order.getId(), isOrderOpen);
        Map<String, ProductTotalOrder> productTotalOrders = productOrderTotal.stream()
                .collect(toMap(ProductTotalOrder::getProduct));

        if (productTotalOrders.isEmpty())
            return new ArrayList<>();

        List<Product> orderedProducts = productService.getProducts(productTotalOrders.keySet());

        Map<String, SupplierOrderItem> supplierOrderItems = supplierOrderItemRepo.findByOrderId(order.getId()).stream()
                .collect(toMap(SupplierOrderItem::getProductId));

        return orderedProducts.stream()
                .map(p -> new OrderByProductDTO().fromModel(p, productTotalOrders.get(p.getId()), supplierOrderItems.get(p.getId())))
                .collect(toList());
    }

    public synchronized Order create(OrderDTO dto) throws GoGasException {
        OrderType orderType = orderTypeService.getRequired(dto.getOrderTypeId());
        log.info("Creating order for type {} ({})", orderType.getDescription(), orderType.getId());

        checkForDuplicates(dto, orderType);
        validateOrderDates(dto);

        Order createdOrder = super.create(dto);

        //this is required to have order type description in push notification
        createdOrder.getOrderType().setDescription(orderType.getDescription());

        notificationSender.sendOrderNotification(createdOrder, OrderEvent.Opened);

        if (dto.isUpdateProductList())
            productService.syncPriceList(orderType);

        return createdOrder;
    }

    public Order update(String orderId, OrderDTO dto) throws ItemNotFoundException {
        validateOrderDates(dto);

        return super.update(orderId, dto);
    }

    private void checkForDuplicates(OrderDTO dto, OrderType orderType) {
        List<Order> duplicateOrders = orderRepo.findByOrderTypeIdAndDueDateAndDeliveryDate(orderType.getId(), dto.getDueDate(), dto.getDeliveryDate());

        if (!duplicateOrders.isEmpty()) {
            log.warn("An order already exists with due date {} and delivery date {}", dto.getDueDate(), dto.getDeliveryDate());
            throw new OrderAlreadyExistsException();
        }
    }

    private void validateOrderDates(OrderDTO dto) {
        if (dto.getDueDate().isBefore(LocalDate.now())) {
            throw new MissingOrInvalidParameterException("Data di chiusura non valida");
        }

        if (!dto.getOpeningDate().isBefore(dto.getDueDate()) || !dto.getDueDate().isBefore(dto.getDeliveryDate())) {
            throw new MissingOrInvalidParameterException("Date ordine non valide");
        }
    }

    public List<OrderDTO> search(OrderSearchFilter searchFilter, String userId, User.Role userRole) {

        List<String> managedOrderTypes = getOrderTypesManagedBy(userId, userRole);

        Specification<Order> filter = new SpecificationBuilder<Order>()
                .withBaseFilter(OrderSpecs.select(DELIVERY_DATE))
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

        Set<String> orderIds = ListConverter.fromList(orderList).extractIds(Order::getId);

        Map<String, OrderSummary> orderSummaries = userOrderSummaryService.getOrdersTotal(orderIds).stream()
                .collect(Collectors.toMap(OrderSummary::getOrderId, Function.identity()));

        return orderList.stream()
                .map(entry -> new OrderDTO().fromModel(entry, orderSummaries.get(entry.getId()),  orderWorkflowHandler.getAvailableActions(entry, User.Role.A)))
                .collect(toList());
    }

    //TODO: cambiare spostando logica in un oggetto utente o referente
    private List<String> getOrderTypesManagedBy(String userId, User.Role userRole) {
        if (userRole.isAdmin())
            return Collections.emptyList();

        return orderManagerRepo.findByUser(userId).stream()
                .map(OrderManager::getOrderType)
                .collect(toList());
    }

    public void changeStatus(String orderId, String actionCode, int roundType) throws ItemNotFoundException, InvalidOrderActionException {
        Order order = this.getRequiredWithType(orderId);
        orderWorkflowHandler.changeStatus(order, actionCode, roundType);
    }

    /*** OPERATIONS *****/

    public List<OrderItemByProductDTO> getOrderItemsByProduct(String orderId, String productId) throws ItemNotFoundException {
        Order order = this.getRequiredWithType(orderId);

        List<ByProductOrderItem> orderItems = orderItemService.getItemsByProduct(productId, orderId, !order.getStatus().isOpen());
        Set<String> orderItemsId = ListConverter.fromList(orderItems).extractIds(ByProductOrderItem::getUser);
        Map<String, String> userFullNameMap = userService.getUsersFullNameMap(orderItemsId);

        return orderItems.stream()
                .map(o -> new OrderItemByProductDTO().fromModel(o, userFullNameMap.get(o.getUser())))
                .sorted(Comparator.comparing(OrderItemByProductDTO::getUser))
                .collect(toList());
    }

    public boolean updateItemDeliveredQty(String orderId, String orderItemId, BigDecimal deliveredQty) {
        Order order = getRequiredWithType(orderId);
        return orderItemService.updateDeliveredQty(orderId, orderItemId, deliveredQty, order.getOrderType().isComputedAmount());
    }

    public void insertOrderItem(String orderId, OrderItemUpdateRequest orderItem) throws GoGasException {
        if (orderItem.getQuantity() == null || orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new GoGasException("Inserire una quantità maggiore di zero");

        Order order = getRequired(orderId);

        if (order.getStatus() != Closed)
            throw new OrderClosedException();

        User user = userService.getRequired(orderItem.getUserId());
        Product product = productService.getRequired(orderItem.getProductId());
        orderItemService.insertItemByManager(user, product, order, orderItem);
    }

    public List<SelectItemDTO> getUsersNotOrdering(String orderId, String productId) throws ItemNotFoundException {
        Set<String> userIds = orderItemService.getUsersWithOrder(orderId, productId);
        return getUsersNotOrdering(orderId, userIds);
    }

    public List<SelectItemDTO> getUsersNotOrdering(String orderId) throws ItemNotFoundException {
        Set<String> userIds = userOrderSummaryService.getUsersWithOrder(orderId);
        return getUsersNotOrdering(orderId, userIds);
    }

    private List<SelectItemDTO> getUsersNotOrdering(String orderId, Set<String> userIds) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        Set<String> userRoles = userService.getAllUserRolesAsString(false, !order.getOrderType().isSummaryRequired());

        if (userIds.isEmpty())
            return userService.getActiveUsersByRoles(userRoles);

        return userService.getActiveUsersForSelectByBlackListAndRoles(userIds, userRoles);
    }

    public void updateSupplierOrderQty(String orderId, String productId, int boxes) {
        supplierOrderItemRepo.updateBoxesByOrderIdAndProductId(orderId, productId, new BigDecimal(boxes));
    }

    @Transactional
    public void cancelProductOrder(String orderId, String productId) {
        orderItemService.cancelOrderItemByOrderAndProduct(orderId, productId);
        supplierOrderItemRepo.updateBoxesByOrderIdAndProductId(orderId, productId, BigDecimal.ZERO);
    }

    public void replaceOrderItemWithProduct(String orderId, String orderItemId, String productId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);

        SupplierOrderItem supplierOrderItem = supplierOrderItemRepo.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new ItemNotFoundException("SupplierOrder", Arrays.asList(orderId, productId)));

        orderItemService.replaceOrderItemsProduct(orderItemId, order.getId(), order.getOrderType().isSummaryRequired(), productId, supplierOrderItem);
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
                .orElse(BigDecimal.ZERO);

        BigDecimal remainingQuantity = supplierOrderItem.getTotalQuantity().subtract(totalDeliveredQuantity);
        BigDecimal remainingQuantityRatio = remainingQuantity.divide(totalDeliveredQuantity, RoundingMode.HALF_UP);

        orderItemService.increaseDeliveredQtyByProduct(orderId, productId, remainingQuantityRatio);
    }

    @Transactional
    public void updateUserCost(String orderId, String userId, BigDecimal cost) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        User user = userService.getRequired(userId);

        userOrderSummaryService.updateUserTotalForNotComputedOrder(order, user, cost);

        if (order.getShippingCost() != null && order.getShippingCost().doubleValue() > 0) {
            distributeShippingCostsOnUserOrders(order);
        }
    }

    @Transactional
    public void deleteUserCost(String orderId, String userId) {
        Order order = getRequiredWithType(orderId);
        userOrderSummaryService.deleteByOrderIdAndUserId(order, userId);
    }

    public List<OrderByUserDTO> getOrderDetailByUser(String orderId) throws ItemNotFoundException {
        return getOrderDetailByUser(getRequiredWithType(orderId));
    }

    private List<OrderByUserDTO> getOrderDetailByUser(Order order) {
        Map<String, UserOrderSummary> userOrderItems = userOrderSummaryService.findAggregatedByOrderId(order.getId()).stream()
                .collect(toMap(UserOrderSummary::getUserId));

        Map<String, BigDecimal> shippingCostMap = shippingCostRepo.findByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ShippingCost::getUserId, ShippingCost::getAmount));

        List<User> users = userService.getFullUsers(userOrderItems.keySet());

         return users.stream()
                 .map(user -> getOrderByUser(user, userOrderItems.get(user.getId()), shippingCostMap.get(user.getId())))
                 .sorted(Comparator.comparing(OrderByUserDTO::getUserFullName))
                 .collect(toList());
    }

    private OrderByUserDTO getOrderByUser(User user, UserOrderSummary userOrderItem, BigDecimal shippingCost) {

        OrderByUserDTO orderByUser = new OrderByUserDTO();
        orderByUser.setUserId(user.getId());
        orderByUser.setUserFullName(userService.getUserDisplayName(user));
        orderByUser.setOrderedItemsCount(userOrderItem != null ? userOrderItem.getItemsCount() : 0);
        orderByUser.setShippingCost(shippingCost);
        orderByUser.setNegativeBalance(user.getBalance().doubleValue() < 0);
        orderByUser.setNetAmount(userOrderItem != null ? userOrderItem.getTotalAmount() : null);
        return orderByUser;
    }

    @Transactional
    public List<OrderByUserDTO> updateShippingCost(String orderId, BigDecimal shippingCost) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        order.setShippingCost(shippingCost);

        return distributeShippingCostsOnUserOrders(order);
    }

    private List<OrderByUserDTO> distributeShippingCostsOnUserOrders(Order order) {
        List<OrderByUserDTO> userOrders = getOrderDetailByUser(order);
        BigDecimal totalOrderAmount = userOrders.stream()
                .map(OrderByUserDTO::getNetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        for (OrderByUserDTO userOrder : userOrders) {
            BigDecimal userShippingCost = BigDecimal.ZERO;

            if (totalOrderAmount.compareTo(BigDecimal.ZERO) > 0 && userOrder.getNetAmount() != null)
                userShippingCost = order.getShippingCost().multiply(userOrder.getNetAmount().divide(totalOrderAmount, 3, RoundingMode.HALF_UP));

            if (userShippingCost.compareTo(BigDecimal.ZERO) > 0) {
                ShippingCost shippingCostEntity = new ShippingCost();
                shippingCostEntity.setOrderId(order.getId());
                shippingCostEntity.setUserId(userOrder.getUserId());
                shippingCostEntity.setAmount(userShippingCost);
                shippingCostRepo.save(shippingCostEntity);
            } else {
                shippingCostRepo.deleteByOrderIdAndUserId(order.getId(), userOrder.getUserId());
            }

            userOrder.setShippingCost(userShippingCost);
        }

        return userOrders;
    }

    public List<OrderItemByUserDTO> getOrderItemsByUser(String orderId, String userId) throws ItemNotFoundException {
        boolean computedAmount = getRequiredWithType(orderId).getOrderType().isComputedAmount();

        Map<String, OpenOrderItem> orderItemsMap = orderItemService.getUserOrderItems(userId, orderId, true);
        List<Product> orderedProducts = productService.getProducts(orderItemsMap.keySet());

        return orderedProducts.stream()
                .map(p -> new OrderItemByUserDTO().fromModel(orderItemsMap.get(p.getId()), p.getDescription(), computedAmount))
                .collect(toList());
    }

    public void updateInvoiceData(String orderId, OrderInvoiceDataDTO invoiceData) throws GoGasException {

        if (invoiceData.getInvoiceAmount() != null && invoiceData.getInvoiceAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new MissingOrInvalidParameterException("L'importo fattura deve essere un valore maggiore di zero");

        Order order = getRequiredWithType(orderId);
        order.setPaymentDate(invoiceData.getPaymentDate());
        order.setPaid(invoiceData.isPaid());

        //if billed by aequos this information should not be modified by the user
        if (!order.getOrderType().isBilledByAequos()) {
            order.setInvoiceAmount(invoiceData.getInvoiceAmount());
            order.setInvoiceNumber(invoiceData.getInvoiceNumber());
            order.setInvoiceDate(invoiceData.getInvoiceDate());
        }

        orderRepo.save(order);
    }

    public void saveInvoiceAttachment(String orderId, byte[] attachmentContent, String contentType) throws GoGasException {
        attachmentService.storeAttachment(attachmentContent, AttachmentType.INVOICE, orderId);
        int rowsUpdated = orderRepo.updateAttachmentType(orderId, contentType);

        if (rowsUpdated < 1)
            throw new ItemNotFoundException("Order", orderId);
    }

    public List<OrderDTO> getAvailableOrdersNotYetOpened(String userId, User.Role userRole) {
        List<OrderDTO> availableOrders = Stream.concat(getAequosAvailableOpenOrders(), orderPlanningService.getWeeklyOrders())
                .collect(toList());

        Set<String> availableOrderTypeIds = availableOrders.stream()
                .map(OrderDTO::getOrderTypeId)
                .collect(Collectors.toSet());

        Set<Integer> statusCodes = Set.of(Opened.getStatusCode(), Closed.getStatusCode());

        Map<String, List<Order>> openOrders = orderRepo.findByOrderTypeIdInAndStatusCodeIn(availableOrderTypeIds, statusCodes)
                .stream().collect(Collectors.groupingBy(order -> order.getOrderType().getId(), toList()));

        List<String> managedOrderTypeIds = getOrderTypesManagedBy(userId, userRole);

        return availableOrders.stream()
                .filter(order -> managedOrderTypeIds.isEmpty() || managedOrderTypeIds.contains(order.getOrderTypeId()))
                .filter(order -> orderNotYetOpened(order, openOrders.get(order.getOrderTypeId())))
                .collect(toList());
    }

    private Stream<OrderDTO> getAequosAvailableOpenOrders() {
        Map<Integer, OrderType> aequosOrderTypeMapping = orderTypeService.getAequosOrderTypesMapping();

        return aequosIntegrationService.getOpenOrders().stream()
                .map(o -> createOrderDTO(aequosOrderTypeMapping.get(o.getId()), o))
                .filter(Objects::nonNull);
    }

    private OrderDTO createOrderDTO(OrderType type, AequosOpenOrder o) {
        if (type == null)
            return null;

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderTypeId(type.getId());
        orderDTO.setOrderTypeName(type.getDescription());
        orderDTO.setOpeningDate(o.getOpeningDate());
        orderDTO.setDueDate(o.getDueDate());
        orderDTO.setDeliveryDate(o.getDeliveryDate());

        return orderDTO;
    }

    private boolean orderNotYetOpened(OrderDTO availableOrder, List<Order> existingOrders) {
        if (existingOrders == null || existingOrders.isEmpty())
            return true;

        return existingOrders.stream()
                .noneMatch(o -> o.getDeliveryDate().isEqual(availableOrder.getDeliveryDate()));
    }

    public OrderDetailsDTO getOrderDetails(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        boolean hasAttachment = attachmentService.hasAttachment(AttachmentType.INVOICE, orderId);
        return new OrderDetailsDTO().fromModel(order, hasAttachment);
    }

    public AttachmentDTO readInvoiceAttachment(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        byte[] attachmentContent = attachmentService.retrieveAttachment(AttachmentType.INVOICE, orderId);
        return attachmentService.buildAttachmentDTO(order, attachmentContent, order.getAttachmentType());
    }

    public AttachmentDTO extractExcelReport(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        return extractExcelReport(order);
    }

    private AttachmentDTO extractExcelReport(Order order) {
        byte[] excelReportContent = reportService.extractOrderDetails(order, aequosIntegrationService.requiresWeightColumns(order));
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return attachmentService.buildAttachmentDTO(order, excelReportContent, contentType);
    }

    public String sendOrderToAequos(String orderId, String currentUserId) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        Integer aequosOrderType = order.getOrderType().getAequosOrderId();
        if (aequosOrderType == null)
            throw new GoGasException(NOT_AEQUOS_ORDER_ERROR_MESSAGE);

        List<SupplierOrderBoxes> supplierOrderBoxes = supplierOrderItemRepo.findBoxesCountByOrderId(order.getId());

        if (supplierOrderBoxes.isEmpty())
            throw new GoGasException("Order cannot be sent: no products found");

        String aequosOrderId = aequosIntegrationService.createOrUpdateOrder(aequosOrderType, order.getExternalOrderId(), supplierOrderBoxes);
        orderRepo.updateOrderExternalId(orderId, aequosOrderId, true);

        if (aequosIntegrationService.requiresWeightColumns(order)) {
            String currentUserEmail = userService.getRequired(currentUserId).getEmail();
            aequosIntegrationService.sendExcelToSupplier(order, currentUserEmail, extractExcelReport(order));
        }

        return aequosOrderId;
    }

    public void sendOrderToAequosMail(String orderId, String currentUserId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        if (aequosIntegrationService.requiresWeightColumns(order)) {
            String currentUserEmail = userService.getRequired(currentUserId).getEmail();
            aequosIntegrationService.sendExcelToSupplier(order, currentUserEmail, extractExcelReport(order));
        }
    }

    @Transactional
    public int sendWeightsToAequos(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        if (order.getOrderType().getAequosOrderId() == null)
            throw new GoGasException(NOT_AEQUOS_ORDER_ERROR_MESSAGE);

        if (order.getExternalOrderId() == null)
            throw new GoGasException("Missing Aequos order id");

        List<ProductTotalOrder> totalDeliveredQuantityByProduct = orderItemService.getTotalQuantityByProduct(order.getId(), false);
        List<String> updatedItems = aequosIntegrationService.sendUpdatedWeights(order.getExternalOrderId(), order.getOrderType().getAequosOrderId(), totalDeliveredQuantityByProduct);

        supplierOrderItemRepo.updateItemsAsWeightSentByOrderAndProduct(orderId, updatedItems);
        orderRepo.updateWeightSentDate(orderId, LocalDateTime.now());

        return updatedItems.size();
    }

    @Transactional
    public boolean synchOrderWithAequos(String orderId, boolean onlyIfInvoiceAvailable) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        if (order.getOrderType().getAequosOrderId() == null)
            throw new GoGasException(NOT_AEQUOS_ORDER_ERROR_MESSAGE);

        if (order.getExternalOrderId() == null)
            throw new GoGasException("Missing Aequos order id");

        log.info("Synchronizing with Aequos order with id {} (Aequos id {}) ", orderId, order.getExternalOrderId());

        OrderSynchResponse orderSynchResponse = aequosIntegrationService.synchronizeOrder(order.getExternalOrderId());

        if (onlyIfInvoiceAvailable && isEmpty(orderSynchResponse.getInvoiceNumber()))
            return false;

        updateSupplierOrderItems(orderId, order.getOrderType().getId(), orderSynchResponse.getOrderItems());
        LocalDate invoiceDate = orderSynchResponse.getInvoiceNumber() != null && !orderSynchResponse.getInvoiceNumber().isEmpty() ? order.getDeliveryDate() : null;
        orderRepo.updateInvoiceDataAndSynchDate(orderId, orderSynchResponse.getInvoiceNumber(), orderSynchResponse.getOrderTotalAmount(), invoiceDate , LocalDateTime.now());

        log.info("Synchronization completed successfully");
        return true;
    }

    private boolean isEmpty(String invoiceNumber) {
        return invoiceNumber == null || invoiceNumber.trim().isEmpty();
    }

    private void updateSupplierOrderItems(String orderId, String orderTypeId, List<OrderSynchItem> aequosOrderItems) {
        Map<String, OrderSynchItem> aequosItemsMap = aequosOrderItems.stream()
                .collect(toMap(OrderSynchItem::getId));

        List<SupplierOrderItem> existingSupplierOrderItems = supplierOrderItemRepo.findByOrderId(orderId);
        for (SupplierOrderItem supplierOrderItem : existingSupplierOrderItems) {
            String productExternalCode = getProductExternalCode(supplierOrderItem);
            OrderSynchItem aequosOrderItem = aequosItemsMap.get(productExternalCode);

            if (updateSupplierOrderItem(orderId, supplierOrderItem, aequosOrderItem))
                aequosItemsMap.remove(productExternalCode);
        }

        List<SupplierOrderItem> newSupplierOrderItems = aequosItemsMap.values().stream()
            .map(aequosItem -> createSupplierOrderItem(orderId, orderTypeId, aequosItem))
            .collect(toList());

        existingSupplierOrderItems.addAll(newSupplierOrderItems);
        existingSupplierOrderItems.removeAll(Collections.singleton(null));
        supplierOrderItemRepo.saveAll(existingSupplierOrderItems);
    }

    //For backward compatibility, old version didn't have external code
    private String getProductExternalCode(SupplierOrderItem supplierOrderItem) {
        if (supplierOrderItem.getProductExternalCode() == null) {
            Product product = productService.getRequired(supplierOrderItem.getProductId());
            supplierOrderItem.setProductExternalCode(product.getExternalId());
        }

        return supplierOrderItem.getProductExternalCode();
    }

    private boolean updateSupplierOrderItem(String orderId, SupplierOrderItem supplierOrderItem, OrderSynchItem aequosOrderItem) {
        if (aequosOrderItem == null) {
            log.info("Missing Aequos order item, supplier order item is cancelled for product with id {} ({})",
                    supplierOrderItem.getProductId(), supplierOrderItem.getProductExternalCode());

            supplierOrderItem.setTotalQuantity(BigDecimal.ZERO);
            supplierOrderItem.setBoxesCount(BigDecimal.ZERO);
            return false;
        }

        supplierOrderItem.setTotalQuantity(aequosOrderItem.getQuantity());
        supplierOrderItem.setBoxesCount(aequosOrderItem.getQuantity().divide(supplierOrderItem.getBoxWeight(), RoundingMode.HALF_UP));

        //if price has been changed by Aequos, the price must be changed in user orders as well
        if (!supplierOrderItem.getUnitPrice().equals(aequosOrderItem.getPrice())) {
            log.info("Price has changed for product with id {} ({}) from {} to {}, updating user orders",
                    supplierOrderItem.getProductId(), supplierOrderItem.getProductExternalCode(),
                    supplierOrderItem.getUnitPrice().doubleValue(), aequosOrderItem.getPrice().doubleValue());

            orderItemService.updatePriceByOrderIdAndProductId(orderId, supplierOrderItem.getProductId(), aequosOrderItem.getPrice());
        }

        supplierOrderItem.setUnitPrice(aequosOrderItem.getPrice());
        return true;
    }

    private SupplierOrderItem createSupplierOrderItem(String orderId, String orderTypeId, OrderSynchItem aequosOrderItem) {
        Optional<Product> product = productService.getByExternalId(orderTypeId, aequosOrderItem.getId());

        // Necessario per evitare errori di sincronia con l'ordine del fresco di novembre 2020 in cui compare un item TRASPORTO0000 inesistente (Trasporto Cartizze?)
        if (product.isEmpty()) {
            log.warn("Product wih external code {} not found, skipping it", aequosOrderItem.getId());
            return null;
        }

        log.info("Creating new supplier order item for product with id {} ({})", product.get().getId(), aequosOrderItem.getId());
        SupplierOrderItem item = new SupplierOrderItem();
        item.setOrderId(orderId);
        item.setProductId(product.get().getId());
        item.setProductExternalCode(aequosOrderItem.getId());
        item.setBoxesCount(aequosOrderItem.getQuantity().divide(product.get().getBoxWeight(), RoundingMode.HALF_UP));
        item.setTotalQuantity(aequosOrderItem.getQuantity());
        item.setUnitPrice(aequosOrderItem.getPrice());
        item.setBoxWeight(product.get().getBoxWeight());

        return item;
    }
}
