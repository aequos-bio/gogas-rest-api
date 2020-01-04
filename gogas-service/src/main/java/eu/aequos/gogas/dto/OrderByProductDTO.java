package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.SupplierOrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class OrderByProductDTO {

    @JsonProperty("idProdotto")
    private String productId;

    @JsonProperty("nomeProdotto")
    private String productName;

    @JsonProperty("categoria")
    private String category;

    @JsonProperty("umProdotto")
    private String unitOfMeasure;

    @JsonProperty("pesoCollo")
    private BigDecimal boxWeight;

    @JsonProperty("prezzoKg")
    private BigDecimal price;

    @JsonProperty("qta")
    private BigDecimal deliveredQty;

    @JsonProperty("numeroOrdinanti")
    private int orderingUsersCount;

    @JsonProperty("qtaOrdinata")
    private BigDecimal orderedQty;

    @JsonProperty("colliOrdinati")
    private BigDecimal orderedBoxes;

    @JsonProperty("annullato")
    private boolean cancelled;

    /** derived values **/

    @JsonProperty("totale")
    public BigDecimal getTotalAmount() {
        return getDeliverdTotalAmount(); //TODO; useless?
    }

    @JsonProperty("totaleConsegnato")
    public BigDecimal getDeliverdTotalAmount() {
        return deliveredQty.multiply(price);
    }

    @JsonProperty("totaleColli")
    public BigDecimal getOrderedTotalAmount() {
        return orderedQty.multiply(price);
    }

    @JsonProperty("colliRisultanti")
    public BigDecimal getDeliveredBoxes() {
        return deliveredQty.divide(boxWeight, RoundingMode.HALF_UP);
    }

    @JsonProperty("totaleDiff")
    public BigDecimal getAmountDifference() {
        return getDeliverdTotalAmount().subtract(getOrderedTotalAmount());
    }

    @JsonProperty("rimanenze")
    public BigDecimal remainingQty() {
        if (orderedQty == null || deliveredQty == null)
            return BigDecimal.ZERO;

        return orderedQty.subtract(deliveredQty);
    }

    /** builder **/
    public OrderByProductDTO fromModel(Product p, ProductTotalOrder totalProductOrder, SupplierOrderItem supplierOrderItem) {
        this.productId = p.getId();
        this.productName = p.getDescription();
        this.category = p.getCategory().getDescription();
        this.unitOfMeasure = p.getUm();

        if (totalProductOrder != null) {
            this.deliveredQty = totalProductOrder.getTotalQuantity();
            this.orderingUsersCount = totalProductOrder.getUserCount().intValue();
            this.cancelled = totalProductOrder.isCancelled();
        } else {
            this.deliveredQty = BigDecimal.ZERO;
        }

        if (supplierOrderItem != null) {
            this.price = supplierOrderItem.getUnitPrice();
            this.boxWeight = supplierOrderItem.getBoxWeight();
            this.orderedQty = supplierOrderItem.getBoxesCount().multiply(supplierOrderItem.getBoxWeight());
            this.orderedBoxes = supplierOrderItem.getBoxesCount();
        } else {
            this.price = p.getPrice();
            this.boxWeight = p.getBoxWeight();
            this.orderedQty = BigDecimal.ZERO;
            this.orderedBoxes = BigDecimal.ZERO;
        }

        return this;
    }

}
