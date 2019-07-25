package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "prodotti")
public class Product {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idprodotto" , columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "prodotto" , nullable = false)
    private String description;

    @Column(name = "um" , nullable = false, length = 15)
    private String um;

    @Column(name = "pesocassa", precision = 18, scale = 2)
    private BigDecimal boxWeight;

    @Column(name = "prezzokg", precision = 18, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "idtipoprod", nullable = false)
    private String type;

    @Column(name = "acquistabile", nullable = false)
    private boolean available;

    @Column(name = "umcollo", length = 15)
    private String boxUm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idproduttore", nullable = false)
    private Supplier supplier;

    @Column(name = "annullato", nullable = false)
    private boolean cancelled;

    @Column(name = "note", length = 2048)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "idcategoria", nullable = false)
    private ProductCategory category;

    @Column(name = "cadenza", length = 50)
    private String frequency;

    @Column(name = "idesterno", length = 50)
    private String externalId;

    @Column(name = "solocollointero", nullable = false)
    private boolean boxOnly;

    @Column(name = "multiplo")
    private BigDecimal multiple;
}
