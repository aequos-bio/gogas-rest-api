package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "categoriaprodotti")
public class ProductCategory {

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

    public ProductCategory withId(String categoryId) {
        this.setId(categoryId);
        return this;
    }
}