package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "ordersummary")
@IdClass(OrderSummaryId.class)
public class OrderSummary implements Serializable {

    @Id
    @Column(name = "iddateordini", columnDefinition="uniqueidentifier", insertable = false, updatable = false)
    private String orderId;

    @Id
    @Column(name = "idutente", columnDefinition="uniqueidentifier", insertable = false, updatable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idutente")
    private User user;

    @Column(name = "totale")
    private BigDecimal totalAmount;

    @Column(name = "spesespedizione")
    private BigDecimal shipmentCost;

    @Column(name = "prodotti")
    private Integer itemsCount;
}
