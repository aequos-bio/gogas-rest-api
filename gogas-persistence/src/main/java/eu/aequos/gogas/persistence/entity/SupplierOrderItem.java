package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "ordinifornitore")
public class SupplierOrderItem {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idrigaordine" , columnDefinition="uniqueidentifier")
    private String id;

    @Column(name = "iddateordine", nullable = false)
    private String orderId;

    @Column(name = "idprodotto", nullable = false)
    private String productId;

    @Column(name = "externalcode")
    private String productExternalCode;

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