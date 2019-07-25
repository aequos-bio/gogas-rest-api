package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity()
@Table(name = "produttori")
public class Supplier {

    @Id
    @Column(name = "idproduttore", nullable = false)
    private String idproduttore;
    @Column(name = "ragionesociale", nullable = true)
    private String ragionesociale;
    @Column(name = "provincia", nullable = true)
    private String provincia;
    @Column(name = "idesterno", nullable = true)
    private String idesterno;

    public Supplier withId(String supplierId) {
        this.idproduttore = supplierId;
        return this;
    }
}