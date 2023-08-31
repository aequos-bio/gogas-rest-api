package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "userordersummary")
@IdClass(UserOrderSummary.Key.class)
public class UserOrderSummary {

    @Data
    public static class Key implements Serializable {
        private String orderId;
        private String userId;

        public Key() {}

        public Key(String orderId, String userId) {
            this.orderId = orderId;
            this.userId = userId;
        }
    }

    @Id
    @Column(name = "orderid" , columnDefinition="uniqueidentifier", nullable = false)
    private String orderId;

    @Id
    @Column(name = "userid" , columnDefinition="uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "friendreferralid" , columnDefinition="uniqueidentifier")
    private String friendReferralId;

    @Column(name = "itemscount")
    private int itemsCount;

    @Column(name = "accounteditemscount")
    private int accountedItemsCount;

    @Column(name = "totalamount")
    private BigDecimal totalAmount;

    @Column(name = "aggregated", nullable = false)
    private boolean aggregated;

    public UserOrderSummary() {}

    public UserOrderSummary(String orderId, String userId) {
        this.orderId = orderId;
        this.userId = userId;
    }
}
