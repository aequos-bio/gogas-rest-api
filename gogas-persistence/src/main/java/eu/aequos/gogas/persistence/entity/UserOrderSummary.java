package eu.aequos.gogas.persistence.entity;

import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "userordersummary")
@IdClass(UserOrderSummary.Key.class)
public class UserOrderSummary implements OrderSummary {

    @Data
    public static class Key implements Serializable {
        private String orderId;
        private String userId;
    }

    @Id
    @Column(name = "orderid" , columnDefinition="uniqueidentifier", nullable = false)
    private String orderId;

    @Id
    @Column(name = "userid" , columnDefinition="uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "itemscount")
    private int itemsCount;

    @Column(name = "frienditemscount")
    private int friendItemsCount;

    @Column(name = "frienditemsaccounted")
    private int friendItemsAccounted;

    @Column(name = "totalamount")
    private BigDecimal totalAmount;

    @Column(name = "shippingcost")
    private BigDecimal shippingCost;

    public UserOrderSummary() {}

    public UserOrderSummary(String orderId, String userId) {
        this.orderId = orderId;
        this.userId = userId;
    }
}
