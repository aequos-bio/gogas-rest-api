package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class OrderDetailsDTO {

    private static final Integer AEQUOS_FRESCO_ORDER_ID = new Integer(0);
    private static final int DAYS_MILLIS = 24 * 3600 * 1000;

    private String id;

    @JsonProperty("idtipoordine")
    private String orderTypeId;

    @JsonProperty("tipoordine")
    private String orderTypeName;

    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

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

    public OrderDetailsDTO fromModel(Order order, boolean hasAttachment) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.deliveryDate = order.getDeliveryDate();
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
        this.weightsSentDate = order.getLastWeightUpdate();
        this.hasAttachment = hasAttachment;
        this.sent = order.isSent();
        this.externalOrderId = order.getExternalOrderId();
        this.syncDate = order.getLastSynchro();

        return this;
    }

    private boolean sendWeightAllowed(LocalDate deliveryDate) {
        long diffInDays = Period.between(deliveryDate, LocalDate.now()).getDays();
        return diffInDays >= 0 && diffInDays <= 4;
    }
}
