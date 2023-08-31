package eu.aequos.gogas.persistence.entity;

import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "utenti")
@NamedStoredProcedureQuery(
    name = "UserExport.balance",
    procedureName = "SaldoContabile_Get_ByIdUtente",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "idUtente", type = String.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "getBalance", type = BigDecimal.class)
    }
)
public final class User implements UserCoreInfo {

    public enum Role {
        U("Utente"),
        S("Amico"),
        A("Amministratore");

        private String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean isFriend() {
            return this == S;
        }

        public boolean isAdmin() {
            return this == A;
        }
    }

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idutente" , columnDefinition="uniqueidentifier")
    private String id;

    @Column(name = "utente", nullable = false, length = 30)
    private String username;

    @Column(name = "pwd", nullable = false, length = 100)
    private String password;

    @Column(name = "ruolo", nullable = false, length = 1)
    private String role;

    @Column(name = "nome", length = 50)
    private String firstName;

    @Column(name = "cognome", length = 50)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "attivo")
    private boolean enabled;

    @Column(name = "telefono", length = 20)
    private String phone;

    @Column(name = "position")
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="idreferente")
    private User friendReferral;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @OneToMany(mappedBy = "user")
    private List<OrderManager> managedOrders;

    public User withUserId(String id) {
        this.id = id;
        return this;
    }

    public static User fromId(String id) {
        return fromId(id, null);
    }

    public static User fromId(String id, String friendReferralId) {
        User user = new User();
        user.setId(id);

        if (friendReferralId != null) {
            user.setFriendReferral(User.fromId(friendReferralId));
            user.setRole(Role.S.name());
        } else {
            user.setRole(Role.U.name());
        }

        return user;
    }

    public Role getRoleEnum() {
        return Role.valueOf(role.toUpperCase());
    }
}
