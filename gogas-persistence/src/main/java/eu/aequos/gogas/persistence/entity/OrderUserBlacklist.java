package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@IdClass(OrderUserBlacklist.Key.class)
@Table(name = "orderuserblacklist")
public class OrderUserBlacklist {
    @Id
    @Column(name = "ordertypeid", columnDefinition="uniqueidentifier", nullable = false)
    private String orderTypeId;

    @Id
    @Column(name = "userid", columnDefinition="uniqueidentifier", nullable = false)
    private String userId;

    @Data
    public static class Key implements Serializable {
        private String orderTypeId;
        private String userId;

        public Key() {}

        public Key(String orderTypeId, String userId) {
            this.orderTypeId = orderTypeId;
            this.userId = userId;
        }
    }
}