package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.UserBalance;
import lombok.Data;

import java.math.BigDecimal;

@Data
public final class UserBalanceDTO {

    @JsonProperty(value = "idUtente")
    private String userId;

    @JsonProperty(value = "Saldo")
    private BigDecimal balance;

    @JsonProperty(value = "nome")
    private String firstName;

    @JsonProperty(value = "cognome")
    private String lastName;

    @JsonProperty(value = "NomeCognome")
    private String fullName;

    @JsonProperty(value = "attivo")
    private boolean enabled;

    public UserBalanceDTO fromModel(UserBalance userBalance, String userFullName) {
        userId = userBalance.getUserId();
        firstName = userBalance.getFirstName();
        lastName = userBalance.getLastName();
        fullName = userFullName;
        enabled = userBalance.isEnabled();
        balance = userBalance.getBalance();

        return this;
    }
}
