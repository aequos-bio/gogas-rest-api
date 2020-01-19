package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class AccountingGasEntryDTO implements ConvertibleDTO<AccountingGasEntry> {

    private String id;

    @JsonProperty(value = "data")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate date;

    @JsonProperty(value = "codicecausale")
    private String reasonCode;

    @JsonProperty(value = "nomecausale")
    private String reasonDescription;

    @JsonProperty(value = "descrizione")
    private String description;

    @JsonProperty(value = "importo")
    private BigDecimal amount;

    @Override
    public AccountingGasEntryDTO fromModel(AccountingGasEntry model) {
        id = model.getId();
        date = model.getDate();
        description = model.getDescription();
        reasonCode = model.getReason().getReasonCode();
        reasonDescription = model.getReason().getDescription() + " (" + model.getReason().getSign() + ")";
        amount = model.getAmount();
        return this;
    }

    @Override
    public AccountingGasEntry toModel(Optional<AccountingGasEntry> existingModel) {
        AccountingGasEntry model = existingModel.orElse(new AccountingGasEntry());

        model.setDate(date);
        model.setDescription(description);
        model.setReason(new AccountingEntryReason().withReasonCode(reasonCode));
        model.setAmount(amount);

        return model;
    }
}
