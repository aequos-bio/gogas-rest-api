package eu.aequos.gogas.order;

import eu.aequos.gogas.dto.OrderInvoiceDataDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ShippingCostRepo;
import eu.aequos.gogas.service.AccountingService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GoGasOrder {

    private static Map<Integer, OrderStatus> orderStatusMap = Arrays.stream(OrderStatus.values())
            .collect(Collectors.toMap(OrderStatus::getStatusCode, Function.identity()));

    protected Order order;
    protected OrderRepo orderRepo;
    protected AccountingService accountingService;
    protected ShippingCostRepo shippingCostRepo;

    public GoGasOrder(Order order, OrderRepo orderRepo, AccountingService accountingService,
                      ShippingCostRepo shippingCostRepo) {

        this.order = order;
        this.orderRepo = orderRepo;
        this.accountingService = accountingService;
        this.shippingCostRepo = shippingCostRepo;
    }

    //TODO: find a way to remove this getter
    public Order getModel() {
        return order;
    }

    public String getId() {
        return order.getId();
    }

    public boolean isOpen() {
        return getStatus().isOpen();
    }

    public boolean isEditable() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(order.getOpeningDate().atStartOfDay()) && now.isBefore(order.getDueDateAndTime());
    }

    public boolean isExternal() {
        return order.getOrderType().isExternal();
    }

    public boolean isSummaryRequired() {
        return order.getOrderType().isSummaryRequired();
    }

    public boolean showAdvance() {
        return order.getOrderType().isShowAdvance();
    }

    public boolean showBoxCompletion() {
        return order.getOrderType().isShowBoxCompletion();
    }

    public boolean showComputedAmount() {
        return order.getOrderType().isComputedAmount();
    }

    public boolean canShowUserAmount() {
        return showAdvance() || order.getStatusCode() == OrderStatus.Accounted.getStatusCode();
    }

    public OrderStatus getStatus() {
        return orderStatusMap.get(order.getStatusCode());
    }

    public String getOrderTypeId() {
        return order.getOrderType().getId();
    }

    public String getOrderTypeDescription() {
        return order.getOrderType().getDescription();
    }

    public LocalDate getDeliveryDate() {
        return order.getDeliveryDate();
    }

    public boolean isExpiring(int minutesBefore) {
        return isDateTimeWithinMinutesFromNow(order.getDueDateAndTime(), minutesBefore);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(order.getDueDateAndTime());
    }

    public boolean isNotYetOpened() {
        return LocalDate.now().isBefore(order.getOpeningDate());
    }

    public boolean isInDelivery(int referenceHour, int minutesBefore) {
        LocalDateTime deliveryDateAndTime = order.getDeliveryDate().atTime(referenceHour, 0);
        return isDateTimeWithinMinutesFromNow(deliveryDateAndTime, minutesBefore);
    }

    private boolean isDateTimeWithinMinutesFromNow(LocalDateTime orderDate, int minutes) {
        long diffInMinutesFromNow = Duration.between(orderDate, LocalDateTime.now()).toMinutes();
        return Math.abs(diffInMinutesFromNow - minutes) < 2;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        order.setShippingCost(shippingCost);
    }

    public void updateStatus(OrderStatus targetStatus) {
        orderRepo.updateOrderStatus(order.getId(), targetStatus.getStatusCode());
    }

    public void updateInvoiceData(OrderInvoiceDataDTO invoiceData) throws GoGasException {
        if (invoiceData.getInvoiceAmount() != null && invoiceData.getInvoiceAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new GoGasException("L'importo fattura deve essere un valore maggiore di zero");

        order.setInvoiceNumber(invoiceData.getInvoiceNumber());
        order.setInvoiceDate(invoiceData.getInvoiceDate());
        order.setInvoiceAmount(invoiceData.getInvoiceAmount());
        order.setPaymentDate(invoiceData.getPaymentDate());
        order.setPaid(invoiceData.isPaid());

        orderRepo.save(order);
    }

    public abstract Map<String, OpenOrderItem> getUserOrderItems(User user);

    public abstract void chargeToUsers();

    public abstract void undoChargeToUsers();

    public abstract Set<String> getChargedUsers();

    public abstract Set<String> getOrderingUsers();

    public abstract List<UserOrderSummary> getUserOrderSummary();

    //////////////////////////////utility

    public static OrderStatus getOrderStatus(int statusCode) {
        return orderStatusMap.get(statusCode);
    }

    public abstract void recomputeAllUsersTotal();

    public void clearOrderManagerData() {
        shippingCostRepo.deleteByOrderId(order.getId());
        orderRepo.clearShippingCost(order.getId());
        clearOrderItems();
    }

    public abstract void clearOrderItems();
}
