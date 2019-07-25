package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "ordini")
public class OrderItem {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idrigaordine" , columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "iddateordine" , columnDefinition="uniqueidentifier", nullable = false)
    private String order;

    @Column(name = "idutente" , columnDefinition="uniqueidentifier", nullable = false)
    private String user;

    @Column(name = "idprodotto" , columnDefinition="uniqueidentifier", nullable = false)
    private String product;

    @Column(name = "idreferenteamico" , columnDefinition="uniqueidentifier")
    private String friendReferral;

    @Column(name = "qtaordinata" , precision = 18, scale = 2, nullable = false)
    private BigDecimal orderedQuantity;

    @Column(name = "um", nullable = false)
    private String um;

    @Column(name = "qtaritiratakg" , precision = 19, scale = 3)
    private BigDecimal deliveredQuantity;

    @Column(name = "prezzokg" , precision = 18, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "annullato", nullable = false)
    private boolean cancelled;

    @Column(name = "riepilogoutente", nullable = false)
    private boolean summary;

    @Column(name = "idprodottosostituito" , columnDefinition="uniqueidentifier")
    private String replacedProduct;

}
