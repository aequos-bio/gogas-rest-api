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
import eu.aequos.gogas.notification.OrderEvent;
import eu.aequos.gogas.notification.push.PushNotificationSender;
import eu.aequos.gogas.persistence.entity.*;
import eu.aequos.gogas.persistence.entity.derived.*;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.persistence.repository.SupplierOrderItemRepo;
import eu.aequos.gogas.persistence.specification.OrderSpecs;
import eu.aequos.gogas.persistence.specification.SpecificationBuilder;
import eu.aequos.gogas.workflow.OrderWorkflowHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
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

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class OrderManagerService extends CrudService<Order, String> {

    private OrderRepo orderRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderItemService orderItemService;
    private SupplierOrderItemRepo supplierOrderItemRepo;
    private ShippingCostRepo shippingCostRepo;

    private OrderWorkflowHandler orderWorkflowHandler;

    private UserService userService;
    private OrderTypeService orderTypeService;
    private ProductService productService;
    private AccountingService accountingService;
    private AequosIntegrationService aequosIntegrationService;
    private PushNotificationSender pushNotificationSender;
    private AttachmentService attachmentService;
    private ExcelGenerationService reportService;

    public OrderManagerService(OrderRepo orderRepo, OrderManagerRepo orderManagerRepo,
                               OrderItemService orderItemService, SupplierOrderItemRepo supplierOrderItemRepo,
                               ShippingCostRepo shippingCostRepo, OrderWorkflowHandler orderWorkflowHandler,
                               UserService userService, OrderTypeService orderTypeService, ProductService productService,
                               AccountingService accountingService, AequosIntegrationService aequosIntegrationService,
                               PushNotificationSender pushNotificationSender, AttachmentService attachmentService,
                               ExcelGenerationService reportService) {

        super(orderRepo, "order");
        this.orderRepo = orderRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderItemService = orderItemService;
        this.supplierOrderItemRepo = supplierOrderItemRepo;
        this.shippingCostRepo = shippingCostRepo;
        this.orderWorkflowHandler = orderWorkflowHandler;
        this.userService = userService;
        this.orderTypeService = orderTypeService;
        this.productService = productService;
        this.accountingService = accountingService;
        this.aequosIntegrationService = aequosIntegrationService;
        this.pushNotificationSender = pushNotificationSender;
        this.attachmentService = attachmentService;
        this.reportService = reportService;
    }

    public Order getRequiredWithType(String id) throws ItemNotFoundException {
        return orderRepo.findByIdWithType(id)
                .orElseThrow(() -> new ItemNotFoundException(type, id));
    }

    public List<OrderByProductDTO> getOrderDetailByProduct(String orderId) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        return getOrderDetailByProduct(order);
    }

    public List<OrderByProductDTO> getOrderDetailByProduct(Order order) throws ItemNotFoundException {
        boolean isOrderOpen = order.getStatus().isOpen();

        List<ProductTotalOrder> productOrderTotal = orderItemService.getTotalQuantityByProduct(order.getId(), isOrderOpen);
        Map<String, ProductTotalOrder> productTotalOrders = ListConverter.fromList(productOrderTotal)
                .toMap(ProductTotalOrder::getProduct);

        if (productTotalOrders.isEmpty())
            return new ArrayList<>();

        List<Product> orderedProducts = productService.getProducts(productTotalOrders.keySet());

        Map<String, SupplierOrderItem> supplierOrderItems = ListConverter.fromList(supplierOrderItemRepo.findByOrderId(order.getId()))
                .toMap(SupplierOrderItem::getProductId);

        return orderedProducts.stream()
                .map(p -> new OrderByProductDTO().fromModel(p, productTotalOrders.get(p.getId()), supplierOrderItems.get(p.getId())))
                .collect(toList());
    }



    public synchronized Order create(OrderDTO dto) throws GoGasException {
        log.info("Creating order for type {}", dto.getOrderTypeId());
        OrderType orderType = orderTypeService.getRequired(dto.getOrderTypeId());

        List<String> duplicateOrders = orderRepo.findByOrderTypeIdAndDueDateAndDeliveryDate(orderType.getId(), dto.getDueDate(), dto.getDeliveryDate());

        if (!duplicateOrders.isEmpty()) {
            log.warn("An order already exists with due date {} and delivery date {}", dto.getDueDate(), dto.getDeliveryDate());
            throw new OrderAlreadyExistsException();
        }

        Order createdOrder = super.create(dto);

        //this is required to have order type description in push notification
        createdOrder.getOrderType().setDescription(orderType.getDescription());

        pushNotificationSender.sendOrderNotification(createdOrder, OrderEvent.Opened);

        if (dto.isUpdateProductList())
            productService.syncPriceList(orderType);

        return createdOrder;
    }

    public List<OrderDTO> search(OrderSearchFilter searchFilter, String userId, User.Role userRole) {

        List<String> managedOrderTypes = getOrderTypesManagedBy(userId, userRole);

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
        Set<String> userIds = orderItemService.getUsersWithOrder(orderId, productId);
        return getUsersNotOrdering(orderId, userIds);
    }

    public List<SelectItemDTO> getUsersNotOrdering(String orderId) throws ItemNotFoundException {
        Set<String> userIds = accountingService.getUsersWithOrder(orderId);
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

    public String updateUserCost(String orderId, String userId, BigDecimal cost) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        User user = userService.getRequired(userId);

        return accountingService.createOrUpdateEntryForOrder(order, user, cost); //TODO: ricalcolare costo trasporto
    }

    public boolean deleteUserCost(String orderId, String userId) {
        return accountingService.deleteUserEntryForOrder(orderId, userId);
    }

    public List<OrderByUserDTO> getOrderDetailByUser(String orderId) throws ItemNotFoundException {
        return getOrderDetailByUser(getRequiredWithType(orderId));
    }

    private List<OrderByUserDTO> getOrderDetailByUser(Order order) {
        boolean computedAmount = order.getOrderType().isComputedAmount();

        Map<String, BigDecimal> accountingEntries = accountingService.getOrderAccountingEntries(order.getId()).stream()
                .collect(Collectors.toMap(entry -> entry.getUser().getId(), AccountingEntry::getAmount));

        Map<String, ByUserOrderItem> userOrderItems = ListConverter.fromList(orderItemService.getItemsCountAndAmountByUser(order.getId()))
                .toMap(ByUserOrderItem::getUserId);

        Map<String, BigDecimal> shippingCostMap = shippingCostRepo.findByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(ShippingCost::getUserId, ShippingCost::getAmount));

        Set<String> allUserIds = Stream.concat(accountingEntries.keySet().stream(), userOrderItems.keySet().stream())
                .collect(Collectors.toSet());

        Map<String, String> users = userService.getUsersFullNameMap(allUserIds);

         return users.entrySet().stream()
                 .map(user -> getOrderByUser(user, userOrderItems.get(user.getKey()), accountingEntries.get(user.getKey()), shippingCostMap.get(user.getKey()), computedAmount))
                 .sorted(Comparator.comparing(OrderByUserDTO::getUserFullName))
                 .collect(toList());
    }

    private OrderByUserDTO getOrderByUser(Map.Entry<String, String> user, ByUserOrderItem userOrderItem,
                                          BigDecimal accountingEntryAmount, BigDecimal shippingCost,
                                          boolean computedAmount) {

        OrderByUserDTO orderByUser = new OrderByUserDTO();
        orderByUser.setUserId(user.getKey());
        orderByUser.setUserFullName(user.getValue());
        orderByUser.setOrderedItemsCount(userOrderItem != null ? userOrderItem.getOrderedItems() : 0);
        orderByUser.setShippingCost(shippingCost);
        orderByUser.setNegativeBalance(false); //TODO: riempire correttamente

        if (computedAmount)
            orderByUser.setNetAmount(userOrderItem.getTotalAmount());
        else
            orderByUser.setNetAmount(Optional.ofNullable(accountingEntryAmount).orElse(BigDecimal.ZERO));

        return orderByUser;
    }

    @Transactional
    public List<OrderByUserDTO> updateShippingCost(String orderId, BigDecimal shippingCost) throws ItemNotFoundException {
        Order order = getRequiredWithType(orderId);
        order.setShippingCost(shippingCost);

        List<OrderByUserDTO> userOrders = getOrderDetailByUser(order);
        BigDecimal totalOrderAmount = userOrders.stream()
                .map(OrderByUserDTO::getNetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        for (OrderByUserDTO userOrder : userOrders) {
            BigDecimal userShippingCost = BigDecimal.ZERO;

            if (totalOrderAmount.compareTo(BigDecimal.ZERO) > 0 && userOrder.getNetAmount() != null)
                userShippingCost = shippingCost.multiply(userOrder.getNetAmount().divide(totalOrderAmount, RoundingMode.HALF_UP));

            if (userShippingCost.compareTo(BigDecimal.ZERO) > 0) {
                ShippingCost shippingCostEntity = new ShippingCost();
                shippingCostEntity.setOrderId(orderId);
                shippingCostEntity.setUserId(userOrder.getUserId());
                shippingCostEntity.setAmount(userShippingCost);
                shippingCostRepo.save(shippingCostEntity);
            } else {
                shippingCostRepo.deleteByOrderIdAndUserId(orderId, userOrder.getUserId());
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
            throw new GoGasException("L'importo fattura deve essere un valore maggiore di zero");

        Order order = getRequiredWithType(orderId);
        order.setInvoiceNumber(invoiceData.getInvoiceNumber());
        order.setInvoiceDate(invoiceData.getInvoiceDate());
        order.setInvoiceAmount(invoiceData.getInvoiceAmount());
        order.setPaymentDate(invoiceData.getPaymentDate());
        order.setPaid(invoiceData.isPaid());

        orderRepo.save(order);
    }

    public void saveInvoiceAttachment(String orderId, byte[] attachmentContent, String contentType) throws GoGasException {
        attachmentService.storeAttachment(attachmentContent, AttachmentType.Invoice, orderId);
        int rowsUpdated = orderRepo.updateAttachmentType(orderId, contentType);

        if (rowsUpdated < 1)
            throw new ItemNotFoundException("Order", orderId);
    }

    public List<OrderDTO> getAequosAvailableOpenOrders(String userId, User.Role userRole) {

        Map<Integer, OrderType> aequosOrderTypeMapping = orderTypeService.getAequosOrderTypesMapping();

        Set<Integer> statusCodes = Stream.of(Order.OrderStatus.Opened, Order.OrderStatus.Closed)
                .map(Order.OrderStatus::getStatusCode)
                .collect(Collectors.toSet());

        Set<String> aequosOrderTypeIds = ListConverter.fromList(aequosOrderTypeMapping.values())
                .extractIds(OrderType::getId);

        Map<String, List<Order>> openOrders = orderRepo.findByOrderTypeIdInAndStatusCodeIn(aequosOrderTypeIds, statusCodes)
                .stream().collect(Collectors.groupingBy(order -> order.getOrderType().getId(), toList()));

        Stream<OrderDTO> aequosOpenOrders = aequosIntegrationService.getOpenOrders().stream()
                .map(o -> createOrderDTO(aequosOrderTypeMapping.get(o.getId()), o))
                .filter(Objects::nonNull);

        List<String> managedOrderTypes = getOrderTypesManagedBy(userId, userRole);
        if (!managedOrderTypes.isEmpty())
            aequosOpenOrders = aequosOpenOrders.filter(managedOrderTypes::contains);

        return aequosOpenOrders
                .filter(o -> orderNotYetOpened(o, openOrders.get(o.getOrderTypeId())))
                .collect(toList());
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

    private boolean orderNotYetOpened(OrderDTO aequosOpenOrder, List<Order> gogasOrders) {
        if (gogasOrders == null || gogasOrders.isEmpty())
            return true;

        return gogasOrders.stream()
                .noneMatch(o -> o.getDeliveryDate().isEqual(aequosOpenOrder.getDeliveryDate()));
    }

    public OrderDetailsDTO getOrderDetails(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        boolean hasAttachment = attachmentService.hasAttachment(AttachmentType.Invoice, orderId);
        return new OrderDetailsDTO().fromModel(order, hasAttachment);
    }

    public AttachmentDTO readInvoiceAttachment(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        byte[] attachmentContent = attachmentService.retrieveAttachment(AttachmentType.Invoice, orderId);
        return attachmentService.buildAttachmentDTO(order, attachmentContent, order.getAttachmentType());
    }

    public AttachmentDTO extractExcelReport(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);
        byte[] excelReportContent = reportService.extractOrderDetails(order);
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return attachmentService.buildAttachmentDTO(order, excelReportContent, contentType);
    }

    public String sendOrderToAequos(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        Integer aequosOrderType = order.getOrderType().getAequosOrderId();
        if (aequosOrderType == null)
            throw new GoGasException("Order type is not linked to Aequos");

        List<SupplierOrderBoxes> supplierOrderBoxes = supplierOrderItemRepo.findBoxesCountByOrderId(order.getId());
        String aequosOrderId = aequosIntegrationService.createOrUpdateOrder(aequosOrderType, order.getExternalOrderId(), supplierOrderBoxes);
        orderRepo.updateOrderExternalId(orderId, aequosOrderId, true);

        return aequosOrderId;
    }

    @Transactional
    public int sendWeightsToAequos(String orderId) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        if (order.getOrderType().getAequosOrderId() == null)
            throw new GoGasException("Order type is not linked to Aequos");

        if (order.getExternalOrderId() == null)
            throw new GoGasException("Missing Aequos order id");

        List<SupplierOrderBoxes> supplierOrderBoxes = supplierOrderItemRepo.findBoxesCountByOrderId(order.getId());
        List<String> updatedItems = aequosIntegrationService.sendUpdatedWeights(order.getExternalOrderId(), supplierOrderBoxes);

        supplierOrderItemRepo.updateItemsAsWeightSentByOrderAndProduct(orderId, updatedItems);
        orderRepo.updateWeightSentDate(orderId, LocalDateTime.now());

        return updatedItems.size();
    }

    @Transactional
    public boolean synchOrderWithAequos(String orderId, boolean onlyIfInvoiceAvailable) throws GoGasException {
        Order order = getRequiredWithType(orderId);

        if (order.getOrderType().getAequosOrderId() == null)
            throw new GoGasException("Order type is not linked to Aequos");

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
        Map<String, OrderSynchItem> aequosItemsMap = ListConverter.fromList(aequosOrderItems)
                .toMap(OrderSynchItem::getId);

        List<SupplierOrderItem> existingSupplierOrderItems = supplierOrderItemRepo.findByOrderId(orderId);
        for (SupplierOrderItem supplierOrderItem : existingSupplierOrderItems) {
            String productExternalCode = getProductExternalCode(supplierOrderItem);
            OrderSynchItem aequosOrderItem = aequosItemsMap.get(productExternalCode);

            if (updateSupplierOrderItem(orderId, supplierOrderItem, aequosOrderItem))
                aequosItemsMap.remove(productExternalCode);
        }

        List<SupplierOrderItem> newSupplierOrderItems = aequosItemsMap
            .values()
            .stream()
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
        if (!product.isPresent()) {
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
