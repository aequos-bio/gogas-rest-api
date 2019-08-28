package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemByUserDTO {

    @JsonProperty("idOrdine")
    String orderItemId;

    @JsonProperty("name")
    String productName;

    @JsonProperty("um")
    String unitOfMeasure;

    @JsonProperty("qtaRitirata")
    BigDecimal deliveredQty;

    @JsonProperty("tot")
    BigDecimal totalAmount;

    @JsonProperty("annullato")
    boolean cancelled;

    /** builder **/
    public OrderItemByUserDTO fromModel(OpenOrderItem orderItem, String productName, boolean computedAmount) {
        this.orderItemId = orderItem.getId();
        this.productName = productName;
        this.unitOfMeasure = orderItem.getUm();
        this.deliveredQty = orderItem.getDeliveredQuantity();
        this.cancelled = orderItem.isCancelled();

        if (computedAmount)
            this.totalAmount = deliveredQty.multiply(orderItem.getPrice());

        return this;
    }
}