package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "produttori")
public class Supplier {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idproduttore" , columnDefinition = "uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "ragionesociale", nullable = true)
    private String name;

    @Column(name = "provincia", nullable = true)
    private String province;

    @Column(name = "idesterno", nullable = true)
    private String externalId;

    public Supplier withId(String supplierId) {
        this.id = supplierId;
        return this;
    }
}