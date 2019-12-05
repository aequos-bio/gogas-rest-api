package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.derived.ByProductOrderItem;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemByProductDTO {

    @JsonProperty("idOrdine")
    String orderItemId;

    @JsonProperty("idUtente")
    String userId;

    @JsonProperty("utente")
    String user;

    @JsonProperty("qta")
    BigDecimal requestedQty;

    @JsonProperty("um")
    String unitOfMeasure;

    @JsonProperty("qtaRitirata")
    BigDecimal deliveredQty;

    @JsonProperty("annullato")
    boolean cancelled;

    /** builder **/
    public OrderItemByProductDTO fromModel(ByProductOrderItem userOrder, String user) {
        this.orderItemId = userOrder.getId();
        this.userId = userOrder.getUser();
        this.user = user;
        this.requestedQty = userOrder.getOrderedQuantity();
        this.unitOfMeasure = userOrder.getUm();
        this.deliveredQty = userOrder.getDeliveredQuantity();
        this.cancelled = userOrder.isCancelled();

        return this;
    }

    public OrderItemByProductDTO itemIdAndQuantity(String orderItemId, BigDecimal deliveredQty) {
        this.orderItemId = orderItemId;
        this.deliveredQty = deliveredQty;

        return this;
    }
}