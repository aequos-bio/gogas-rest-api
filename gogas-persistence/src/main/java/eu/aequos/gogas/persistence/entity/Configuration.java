package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "configurazione")
public class Configuration {

    @Id
    @Column(name = "chiave", nullable = false)
    private String key;

    @Column(name = "valore", nullable = false)
    private String value;

    @Column(name = "descrizione")
    private String description;

    @Column(name = "visibile", nullable = false)
    private boolean visible;
}