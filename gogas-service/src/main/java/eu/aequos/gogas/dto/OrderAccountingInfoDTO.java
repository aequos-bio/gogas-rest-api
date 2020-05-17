package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderAccountingInfoDTO {
    private String accountingCode;
    private String description;
    private BigDecimal invoiceAmount;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate paymentDate;
    private boolean paid;
    private List<String> orderIds = new ArrayList<>();

    public OrderAccountingInfoDTO fromOrder(Order order) {
        accountingCode = order.getOrderType().getAccountingCode();
        invoiceNumber = order.getInvoiceNumber();
        invoiceDate = order.getInvoiceDate();
        invoiceAmount = order.getInvoiceAmount();
        description = order.getOrderType().getDescription();
        paymentDate = order.getPaymentDate();
        paid = order.isPaid();
        orderIds.add(order.getId());
        return this;
    }

    public BigDecimal getInvoiceAmount() {
        if (invoiceAmount == null)
            return BigDecimal.ZERO;

        return invoiceAmount;
    }

    public InvoiceKey getInvoiceKey() {
        return new InvoiceKey(accountingCode, invoiceNumber, invoiceDate);
    }

    public static OrderAccountingInfoDTO mergeByInvoiceKey(OrderAccountingInfoDTO o1, OrderAccountingInfoDTO o2) {
        OrderAccountingInfoDTO merged = new OrderAccountingInfoDTO();
        merged.setAccountingCode(o1.getAccountingCode());
        merged.setInvoiceNumber(o1.getInvoiceNumber());
        merged.setInvoiceDate(o1.getInvoiceDate());
        merged.setInvoiceAmount(o1.getInvoiceAmount().add(o2.getInvoiceAmount()));
        merged.setDescription(o1.getDescription() + ", " + o2.getDescription());
        merged.setPaymentDate(o1.getPaymentDate() != null ? o1.getPaymentDate() : o2.getPaymentDate());
        merged.setPaid(o1.isPaid() || o2.isPaid());
        merged.getOrderIds().addAll(o1.orderIds);
        merged.getOrderIds().addAll(o2.orderIds);
        return merged;
    }

    @Data
    public static class InvoiceKey {
        private final String accountingCode;
        private final String invoiceNumber;
        private final LocalDate invoiceDate;
    }
}
