package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.UserBalanceEntry;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class UserBalanceEntryDTO {

    @JsonProperty(value = "idRiga")
    private String id;

    @JsonProperty(value = "descrizione")
    private String description;

    @JsonProperty(value = "data")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private Date date;

    @JsonProperty(value = "importo")
    private BigDecimal amount;

    public UserBalanceEntryDTO fromModel(UserBalanceEntry model) {
        id = model.getId();
        date = model.getDate();
        description = model.getDescription();
        amount = getSignedAmount(model.getAmount(), model.getSign());
        return this;
    }

    private BigDecimal getSignedAmount(BigDecimal unsignedAmount, String sign) {
        return AccountingEntryReason.Sign.fromSymbol(sign)
                .getSignedAmount(unsignedAmount);
    }
}
