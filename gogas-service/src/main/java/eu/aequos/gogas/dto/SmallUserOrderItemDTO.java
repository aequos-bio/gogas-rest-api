package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Data
public class SmallUserOrderItemDTO {

    @JsonProperty("umcollo")
    String boxUnitOfMeasure;

    @JsonProperty("price")
    BigDecimal unitPrice;

    @JsonProperty("weight")
    BigDecimal boxWeight;

    @JsonProperty("qta")
    BigDecimal orderRequestedQty;

    @JsonIgnore()
    String orderUnitOfMeasure;

    @JsonProperty("qtaRitirata")
    BigDecimal orderDeliveredQty;

    @JsonIgnore()
    BigDecimal productTotalOrderedQty;

    /** Derived values **/

    @JsonProperty("tot")
    public BigDecimal getOrderTotalAmount() {
        return computeOrderAmountFromDeliveredQty()
                .orElse(computeOrderAmountFromRequestedQty());
    }

    @JsonProperty("colliOrdinati")
    public int completeBoxesCount() {
        return productNotOrdered() ? 0 : productTotalOrderedQty.divide(boxWeight, RoundingMode.HALF_UP).intValue();
    }

    @JsonProperty("kgRimanenti")
    public BigDecimal boxCompletedUnits() {
        return productNotOrdered() ? BigDecimal.ZERO : productTotalOrderedQty.remainder(boxWeight);
    }

    @JsonProperty("kgMancanti")
    public BigDecimal boxAvailableUnits() {
        return productNotOrdered() ? BigDecimal.ZERO : boxWeight.subtract(boxCompletedUnits());
    }

    /** Utility methods **/

    private Optional<BigDecimal> computeOrderAmountFromDeliveredQty() {
        if (orderDeliveredQty == null)
            return Optional.empty();

        return Optional.of(unitPrice.multiply(orderDeliveredQty));
    }

    private BigDecimal computeOrderAmountFromRequestedQty() {
        if (orderRequestedQty == null)
            return null;

        //requested quantity is expressed in boxes
        if (boxUnitOfMeasure != null && boxUnitOfMeasure.equals(orderUnitOfMeasure))
            return unitPrice.multiply(orderRequestedQty).multiply(boxWeight);

        return unitPrice.multiply(orderRequestedQty);
    }

    private boolean productNotOrdered() {
        return productTotalOrderedQty == null || productTotalOrderedQty.equals(BigDecimal.ZERO);
    }

    /** builder **/
    public SmallUserOrderItemDTO fromModel(Product p, OpenOrderItem userOrder, ProductTotalOrder totalProductOrder) {
        this.boxUnitOfMeasure = p.getBoxUm();
        this.unitPrice = p.getPrice();
        this.boxWeight = p.getBoxWeight();

        if (userOrder != null) {
            this.unitPrice =  userOrder.getPrice();
            this.orderRequestedQty = userOrder.getOrderedQuantity();
            this.orderUnitOfMeasure = userOrder.getUm();
            this.orderDeliveredQty = userOrder.getDeliveredQuantity();
        }

        if (totalProductOrder != null)
            this.productTotalOrderedQty = totalProductOrder.getTotalQuantity();

        return this;
    }
}