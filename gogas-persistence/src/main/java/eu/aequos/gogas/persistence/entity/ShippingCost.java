package eu.aequos.gogas.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity()
@Table(name = "spesetrasporto")
@IdClass(ShippingCost.Key.class)
public class ShippingCost {
    @Data
    public static class Key implements Serializable {
        private String orderId;
        private String userId;
    }
    
    @Id
    @Column(name = "idDateOrdini", nullable = false)
    private String orderId;
    
    @Id
    @Column(name = "idutente", nullable = false)
    private String userId;
    
    @Column(name = "importo", nullable = false)
    private BigDecimal amount;
}