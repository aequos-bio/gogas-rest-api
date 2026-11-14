package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class OrderDetailsDTO {

    private static final Integer AEQUOS_FRESCO_ORDER_ID = 0;
    private static final int DAYS_MILLIS = 24 * 3600 * 1000;

    private String id;

    @JsonProperty("idtipoordine")
    private String orderTypeId;

    @JsonProperty("tipoordine")
    private String orderTypeName;

    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    @JsonProperty("datachiusura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate dueDate;

    @JsonProperty("idaequos")
    private Integer aequosId;

    private boolean editable;

    private boolean external;

    @JsonProperty("totaleCalcolato")
    private boolean computedAmount;

    @JsonProperty("speseTrasporto")
    private BigDecimal shippingCost;

    @JsonProperty("contabilizzato")
    private boolean accounted;

    @JsonProperty("numerofattura")
    private String invoiceNumber;

    @JsonProperty("datafattura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate invoiceDate;

    @JsonProperty("totalefattura")
    private BigDecimal invoiceAmount;

    @JsonProperty("evaso")
    private boolean paid;

    @JsonProperty("datapagamento")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate paymentDate;

    @JsonProperty("invioOrdineConsentito")
    private boolean sendOrderAllowed;

    @JsonProperty("invioPesiRichiesto")
    private boolean sendWeightsRequired;

    @JsonProperty("invioPesiConsentito")
    private boolean sendWeightsAllowed;

    @JsonProperty("pesiInviati")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDateTime weightsSentDate;

    private boolean hasAttachment;

    @JsonProperty("inviato")
    private boolean sent;

    @JsonProperty("numeroOrdineEsterno")
    private String externalOrderId;

    @JsonProperty("sincronizzato")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDateTime syncDate;

    @JsonProperty("dataapertura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate openingDate;

    /** Grand total of the order (sum of user/product amounts). */
    @JsonProperty("totaleOrdine")
    private BigDecimal totalAmount;

    /** Number of distinct users that placed an order. */
    @JsonProperty("numeroUtenti")
    private Integer userCount;

    /** Number of distinct products ordered. */
    @JsonProperty("numeroProdotti")
    private Integer productCount;

    /** Available workflow actions for the current user (comma-separated). */
    private String actions;

    public OrderDetailsDTO withActions(java.util.List<String> availableActions) {
        this.actions = String.join(",", availableActions);
        return this;
    }

    public OrderDetailsDTO withMetadata(BigDecimal totalAmount, int userCount, int productCount) {
        this.totalAmount = totalAmount;
        this.userCount = userCount;
        this.productCount = productCount;
        return this;
    }

    public OrderDetailsDTO fromModel(Order order, boolean hasAttachment) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.deliveryDate = order.getDeliveryDate();
        this.dueDate = order.getDueDate();
        this.openingDate = order.getOpeningDate();
        this.aequosId = order.getOrderType().getAequosOrderId();
        this.computedAmount = order.getOrderType().isComputedAmount();
        this.shippingCost = order.getShippingCost();
        this.editable = order.getStatus().equals(Order.OrderStatus.Closed);
        this.accounted = order.getStatus().equals(Order.OrderStatus.Accounted);
        this.external = order.getOrderType().isExternal();
        this.invoiceNumber = order.getInvoiceNumber();
        this.invoiceDate = order.getInvoiceDate();
        this.invoiceAmount = order.getInvoiceAmount();
        this.paid = order.isPaid();
        this.paymentDate = order.getPaymentDate();
        this.sendWeightsRequired = AEQUOS_FRESCO_ORDER_ID.equals(order.getOrderType().getAequosOrderId());
        this.sendWeightsAllowed = sendWeightAllowed(order.getDeliveryDate());
        this.sendOrderAllowed = sendOrderAllowed(order);
        this.weightsSentDate = order.getLastWeightUpdate();
        this.hasAttachment = hasAttachment;
        this.sent = order.isSent();
        this.externalOrderId = order.getExternalOrderId();
        this.syncDate = order.getLastSynchro();

        return this;
    }

    private boolean sendWeightAllowed(LocalDate deliveryDate) {
        long diffInDays = ChronoUnit.DAYS.between(deliveryDate, LocalDate.now());
        return diffInDays >= 0 && diffInDays <= 4;
    }

    private boolean sendOrderAllowed(Order order) {
        if (order.getOrderType().getAequosOrderId() == null) {
            return false;
        }

        long diffInDays = ChronoUnit.DAYS.between(order.getDueDate(), LocalDate.now());
        return diffInDays <= 1;
    }
}
