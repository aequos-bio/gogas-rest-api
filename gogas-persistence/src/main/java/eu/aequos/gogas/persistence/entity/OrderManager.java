package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "responsabili")
public class OrderManager {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idresponsabili" , columnDefinition="uniqueidentifier")
    private String id;

    @Column(name = "idtipologiaordine", nullable = false)
    private String orderType;

    @Column(name = "idutente", nullable = false)
    private String user;
}