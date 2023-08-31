package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummaryDerived;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDTO implements ConvertibleDTO<Order> {

    private String id;

    @JsonProperty("idtipoordine")
    private String orderTypeId;

    @JsonProperty("tipoordine")
    private String orderTypeName;

    @NotNull
    @JsonProperty("dataapertura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate openingDate;

    @NotNull
    @JsonProperty("datachiusura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate dueDate;

    @NotNull
    @JsonProperty("orachiusura")
    private int dueHour;

    @NotNull
    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    @JsonProperty("codicestato")
    private int statusCode;

    @JsonProperty("stato")
    private String statusName;

    @JsonProperty("inviato")
    private boolean sent;

    @JsonProperty("evaso")
    private boolean paid;

    @JsonProperty("totaleordine")
    private BigDecimal totalAmount;

    @JsonProperty("numarticoli")
    private int itemsCount;

    @JsonProperty("totalefattura")
    private BigDecimal invoiceAmount;

    private boolean external;

    @JsonProperty("externallink")
    private String externalLink;

    @JsonProperty("amici")
    private boolean hasFriends;

    @JsonProperty("contabilizzato")
    private boolean accounted;

    @JsonProperty("contabilizzabile")
    private boolean accountable;

    //output only
    private String actions;

    //input only
    private boolean updateProductList;

    public OrderDTO fromModel(Order order, OrderSummary totalAmount, List<String> actions) {
        fromModel(order);

        this.totalAmount = computeTotalAmount(totalAmount, order.getShippingCost());
        this.actions = String.join(",", actions);

        return this;
    }

    private BigDecimal computeTotalAmount(OrderSummary totalAmount, BigDecimal shippingCost) {
        if (totalAmount == null || totalAmount.getTotalAmount() == null)
            return BigDecimal.ZERO;

        if (shippingCost == null)
            return totalAmount.getTotalAmount();

        return totalAmount.getTotalAmount().add(shippingCost);
    }

    public OrderDTO fromModel(Order order, UserOrderSummaryDerived userOrderSummary) {
        fromModel(order);

        BigDecimal shippingCost = null;

        if (userOrderSummary != null) {
            this.itemsCount = userOrderSummary.getItemsCount();
            this.hasFriends = userOrderSummary.getFriendCount() > 0;
            this.accounted = userOrderSummary.getFriendAccounted() > 0;
            shippingCost = userOrderSummary.getShippingCost();
        }

        this.accountable = !order.getStatus().isOpen() && order.getOrderType().isSummaryRequired();
        this.totalAmount = computeTotalAmount(userOrderSummary, shippingCost);

        return this;
    }

    @Override
    public OrderDTO fromModel(Order order) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.openingDate = order.getOpeningDate();
        this.dueDate = order.getDueDate();
        this.dueHour = order.getDueHour();
        this.deliveryDate = order.getDeliveryDate();
        this.statusCode = order.getStatusCode();
        this.statusName = order.getStatus().getDescription();
        this.sent = order.isSent();
        this.paid = order.isPaid();
        this.invoiceAmount = order.getInvoiceAmount();
        this.external = order.getOrderType().isExternal();
        this.externalLink = order.getExternaLlink();

        return this;
    }

    @Override
    public Order toModel(Optional<Order> existingOrder) {
        Order model = existingOrder.orElse(newOrder(this.orderTypeId));

        model.setOpeningDate(this.openingDate);
        model.setDueDate(this.dueDate);
        model.setDueHour(this.dueHour);
        model.setDeliveryDate(this.deliveryDate);
        model.setExternaLlink(this.externalLink);

        return model;
    }

    private Order newOrder(String orderTypeId) {
        OrderType orderType = new OrderType();
        orderType.setId(orderTypeId);

        Order newOrder = new Order();
        newOrder.setOrderType(orderType);
        newOrder.setStatusCode(Order.OrderStatus.Opened.getStatusCode());
        newOrder.setPaid(false);
        newOrder.setShippingCost(BigDecimal.ZERO);
        return newOrder;
    }
}
