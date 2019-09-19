package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "movimentigas")
public class AccountingGasEntry {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "id" , columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "data", nullable = false)
    private Date date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "causale", nullable = false)
    private AccountingEntryReason reason;

    @Column(name = "descrizione", nullable = false, length = 100)
    private String description;

    @Column(name = "importo", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
}
