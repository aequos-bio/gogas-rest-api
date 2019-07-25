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
public class UserOrderItemDTO {

    @JsonProperty("idOrdine")
    String orderId;

    @JsonProperty("utente")
    String userName;

    @JsonProperty("idUtente")
    String userId;

    @JsonProperty("idProdotto")
    String productId;

    @JsonProperty("name")
    String productName;

    String category;
    String categoryColor;

    @JsonProperty("umcollo")
    String boxUnitOfMeasure;

    @JsonProperty("umunita")
    String unitOfMeasure;

    @JsonProperty("price")
    BigDecimal unitPrice;

    @JsonProperty("weight")
    BigDecimal boxWeight;

    @JsonProperty("solocolli")
    boolean boxesOnly;

    @JsonProperty("multiplo")
    BigDecimal orderMultiple;

    @JsonProperty("note")
    String productNotes;

    @JsonProperty("provincia")
    String productProvince;

    @JsonProperty("qta")
    BigDecimal orderRequestedQty;

    @JsonIgnore()
    String orderUnitOfMeasure;

    @JsonProperty("qtaRitirata")
    BigDecimal orderDeliveredQty;

    @JsonProperty("annullato")
    boolean orderCancelled;

    @JsonIgnore()
    BigDecimal productTotalOrderedQty;

    @JsonProperty("sommaOrdiniAmici")
    BigDecimal friendOrderSum;

    @JsonProperty("contabilizzato")
    boolean accounted;


    /** Derived values **/

    @JsonProperty("um")
    public String getOrderUnitOfMeasure() {
        return Optional.ofNullable(orderUnitOfMeasure).orElse(defaultUnitOfMeasure());
    }

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

    private String defaultUnitOfMeasure() {
        if (boxesOnly && boxUnitOfMeasure != null)
            return boxUnitOfMeasure;

        return unitOfMeasure;
    }

    private Optional<BigDecimal> computeOrderAmountFromDeliveredQty() {
        if (orderDeliveredQty == null)
            return Optional.empty();

        return Optional.of(unitPrice.multiply(orderDeliveredQty));
    }

    private BigDecimal computeOrderAmountFromRequestedQty() {
        if (orderRequestedQty == null)
            return null;

        //requested quantity is expressed in boxes
        if (boxUnitOfMeasure != null && boxUnitOfMeasure.equals(unitOfMeasure))
            return unitPrice.multiply(orderRequestedQty).multiply(boxWeight);

        return unitPrice.multiply(orderRequestedQty);
    }

    private boolean productNotOrdered() {
        return productTotalOrderedQty == null || productTotalOrderedQty.equals(BigDecimal.ZERO);
    }
    
    /** builder **/
    public UserOrderItemDTO fromModel(Product p, OpenOrderItem userOrder, ProductTotalOrder totalProductOrder) {
        this.productId = p.getId();
        this.productName = p.getDescription();
        this.category = p.getCategory().getDescription();
        this.categoryColor = p.getCategory().getPriceListColor();
        this.boxUnitOfMeasure = p.getBoxUm();
        this.unitOfMeasure = p.getUm();
        this.unitPrice = p.getPrice();
        this.boxWeight = p.getBoxWeight();
        this.boxesOnly = p.isBoxOnly();
        this.orderMultiple = p.getMultiple();
        this.productNotes = p.getNotes();
        this.productProvince = p.getSupplier().getProvincia();

        if (userOrder != null) {
            this.unitPrice =  userOrder.getPrice();
            this.orderRequestedQty = userOrder.getOrderedQuantity();
            this.orderUnitOfMeasure = userOrder.getUm();
            this.orderDeliveredQty = userOrder.getDeliveredQuantity();
            this.orderCancelled = userOrder.isCancelled();
        }

        if (totalProductOrder != null)
            this.productTotalOrderedQty = totalProductOrder.getTotalQuantity();

        return this;
    }
}