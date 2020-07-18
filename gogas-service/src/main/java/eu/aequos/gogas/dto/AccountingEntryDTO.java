package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class AccountingEntryDTO implements ConvertibleDTO<AccountingEntry> {

    private String id;

    @JsonProperty(value = "data")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate date;

    @JsonProperty(value = "idutente")
    private String userId;

    @JsonProperty(value = "nomeutente")
    private String userName;

    @JsonProperty(value = "codicecausale")
    private String reasonCode;

    @JsonProperty(value = "nomecausale")
    private String reasonDescription;

    @JsonProperty(value = "descrizione")
    private String description;

    @JsonProperty(value = "importo")
    private BigDecimal amount;

    @JsonProperty(value = "segno")
    private String sign;

    private String friendReferralId;

    @Override
    public AccountingEntryDTO fromModel(AccountingEntry model) {
        id = model.getId();
        date = model.getDate();
        userId = model.getUser().getId();
        userName = model.getUser().getFirstName() + " " + model.getUser().getLastName();
        description = model.getDescription();
        reasonCode = model.getReason().getReasonCode();
        reasonDescription = model.getReason().getDescription();
        sign = model.getReason().getSign();
        amount = model.getAmount();
        return this;
    }

    public AccountingEntryDTO fromModel2(AccountingEntry model) {
        id = model.getId();
        date = model.getDate();
        userId = model.getUser().getId();
        userName = model.getUser().getFirstName() + " " + model.getUser().getLastName();
        description = model.getDescription();
        reasonCode = model.getReason().getReasonCode();
        reasonDescription = model.getReason().getDescription();
        amount = model.getAmount();
        sign = model.getReason().getSign();
        return this;
    }

    @Override
    public AccountingEntry toModel(Optional<AccountingEntry> existingModel) {
        AccountingEntry model = existingModel.orElse(new AccountingEntry());

        model.setDate(date);
        model.setUser(new User().withUserId(userId));
        model.setDescription(description);
        model.setReason(new AccountingEntryReason().withReasonCode(reasonCode));
        model.setAmount(amount);
        model.setConfirmed(true);
        model.setFriendReferralId(friendReferralId);

        return model;
    }
}
