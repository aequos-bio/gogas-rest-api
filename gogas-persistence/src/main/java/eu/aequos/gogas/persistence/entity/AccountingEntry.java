package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "movimenti")
@NamedStoredProcedureQuery(
        name = "User.balance",
        procedureName = "SaldoContabile_Get_ByIdUtente",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "idUtente", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "saldo", type = BigDecimal.class)
        }
)
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
    private Date date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "causale", nullable = false)
    private AccountingEntryReason reason;

    @Column(name = "idreferente", columnDefinition="uniqueidentifier")
    private String friendReferralId;

    @Column(name = "descrizione", nullable = false, length = 100)
    private String description;

    @Column(name = "importo", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "confermato", nullable = false)
    private boolean confirmed;

    @Column(name = "iddateordini", columnDefinition="uniqueidentifier")
    private String orderId;
}
