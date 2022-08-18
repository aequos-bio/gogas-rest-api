package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.derived.FriendTotalOrder;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderItem;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
public class UserOrderItemDTO extends SmallUserOrderItemDTO {

    @JsonProperty("idProdotto")
    String productId;

    @JsonProperty("name")
    String productName;

    String category;
    String categoryColor;

    @JsonProperty("umunita")
    String unitOfMeasure;

    @JsonProperty("solocolli")
    boolean boxesOnly;

    @JsonProperty("multiplo")
    BigDecimal orderMultiple;

    @JsonProperty("note")
    String productNotes;

    @JsonProperty("provincia")
    String productProvince;

    @JsonProperty("annullato")
    boolean orderCancelled;

    @JsonProperty("sommaOrdiniAmici")
    BigDecimal friendOrderSum;

    @JsonProperty("contabilizzato")
    boolean accounted;


    /** Derived values **/

    @JsonProperty("um")
    public String getOrderUnitOfMeasure() {
        return Optional.ofNullable(orderUnitOfMeasure).orElse(defaultUnitOfMeasure());
    }

    /** Utility methods **/

    private String defaultUnitOfMeasure() {
        if (boxesOnly && boxUnitOfMeasure != null)
            return boxUnitOfMeasure;

        return unitOfMeasure;
    }
    
    /** builder **/
    public UserOrderItemDTO fromModel(Product p, OpenOrderItem userOrder, ProductTotalOrder totalProductOrder) {
        super.fromModel(p, userOrder, totalProductOrder);

        this.productId = p.getId();
        this.productName = p.getDescription();
        this.category = p.getCategory().getDescription();
        this.categoryColor = p.getCategory().getPriceListColor();
        this.unitOfMeasure = p.getUm();
        this.boxesOnly = p.isBoxOnly();
        this.orderMultiple = p.getMultiple();
        this.productNotes = p.getNotes();
        this.productProvince = p.getSupplier().getProvince();

        if (userOrder != null)
            this.orderCancelled = userOrder.isCancelled();

        return this;
    }

    /** builder **/
    public UserOrderItemDTO fromModel(Product p, OpenOrderItem userOrder, FriendTotalOrder totalFriendOrder) {
        super.fromModel(p, userOrder, null);

        this.productId = p.getId();
        this.productName = p.getDescription();
        this.category = p.getCategory().getDescription();
        this.categoryColor = p.getCategory().getPriceListColor();
        this.unitOfMeasure = p.getUm();
        this.boxesOnly = p.isBoxOnly();
        this.orderMultiple = p.getMultiple();
        this.productNotes = p.getNotes();
        this.productProvince = p.getSupplier().getProvince();

        if (userOrder != null)
            this.orderCancelled = userOrder.isCancelled();

        if (totalFriendOrder != null) {
            this.accounted = totalFriendOrder.isAccounted();
            this.friendOrderSum = Optional.ofNullable(totalFriendOrder.getTotalQuantity())
                    .orElse(BigDecimal.ZERO);
        }

        return this;
    }
}