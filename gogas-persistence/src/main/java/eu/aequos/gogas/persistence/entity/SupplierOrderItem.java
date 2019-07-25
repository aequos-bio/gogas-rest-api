package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "ordinifornitore")
public class SupplierOrderItem {

    @Id
    @Column(name = "idrigaordine", nullable = false)
    private String id;

    @Column(name = "iddateordine", nullable = false)
    private String orderId;

    @Column(name = "idprodotto", nullable = false)
    private String productId;

    @Column(name = "numerocolli", nullable = false)
    private BigDecimal boxesCount;

    @Column(name = "qtacollo", nullable = false)
    private BigDecimal boxWeight;

    @Column(name = "prezzokg", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "qtaordinata", nullable = false)
    private BigDecimal totalQuantity;

    @Column(name = "weightupdated", nullable = false)
    private boolean weightUpdated;
}