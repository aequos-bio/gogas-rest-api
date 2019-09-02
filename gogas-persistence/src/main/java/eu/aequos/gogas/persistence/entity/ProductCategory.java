package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "categoriaprodotti")
public class ProductCategory {

    private static final String DEFAULT_PRICELIST_COLOR = "FFFFFF";
    private static final int DEFAULT_PRICELIST_POSITION = 1;

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idcategoriaprodotto", columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "idtipologiaordine", nullable = false)
    private String orderTypeId;

    @Column(name = "descrizione", nullable = false)
    private String description;

    @Column(name = "ordine_listino", nullable = false)
    private int priceListPosition;

    @Column(name = "colore_listino", nullable = true)
    private String priceListColor;

    public ProductCategory() {}

    public ProductCategory(String orderTypeId, String description) {
        this.orderTypeId = orderTypeId;
        this.description = description;
        this.priceListColor = DEFAULT_PRICELIST_COLOR;
        this.priceListPosition = DEFAULT_PRICELIST_POSITION;
    }

    public ProductCategory withId(String categoryId) {
        this.setId(categoryId);
        return this;
    }
}