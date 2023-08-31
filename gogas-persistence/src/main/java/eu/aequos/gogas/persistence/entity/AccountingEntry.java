package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "movimenti")
public class AccountingEntry {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idmovimento" , columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idutente", columnDefinition="uniqueidentifier", nullable = false)
    private User user;

    @Column(name = "datamovimento", nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "causale", nullable = false)
    private AccountingEntryReason reason;

    @Column(name = "idreferente", columnDefinition="uniqueidentifier")
    private String friendReferralId;

    @Column(name = "descrizione", nullable = false, length = 100)
    private String description;

    @Column(name = "importo", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    //WARNING: this is now used to exclude accounting entries created for computed orders from "schedacontabile" to have a comparison after migration
    @Column(name = "confermato", nullable = false)
    private boolean confirmed;

    @Column(name = "iddateordini", columnDefinition="uniqueidentifier")
    private String orderId;
}
