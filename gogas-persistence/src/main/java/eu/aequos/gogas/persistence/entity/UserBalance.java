package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "saldocontabile")
@NamedStoredProcedureQuery(
        name = "User.balance",
        procedureName = "SaldoContabile_Get_ByIdUtente",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "idUtente", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "saldo", type = BigDecimal.class)
        }
)
public class UserBalance {

    @Id
    @Column(name = "idutente", columnDefinition="uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "saldo", nullable = false)
    private BigDecimal balance;

    @Column(name = "nome", length = 50)
    private String firstName;

    @Column(name = "cognome", length = 50)
    private String lastName;

    @Column(name = "ruolo", nullable = false, length = 1)
    private String role;

    @Column(name = "attivo")
    private boolean enabled;

    @Column(name="idreferente")
    private String referralId;
}
