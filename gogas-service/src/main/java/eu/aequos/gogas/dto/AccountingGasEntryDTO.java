package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import eu.aequos.gogas.persistence.entity.derived.OrderTotal;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class AccountingGasEntryDTO implements ConvertibleDTO<AccountingGasEntry> {
    public static int SUPPLIER_INVOICE = 1;
    public static int SUPPLIER_PAYMENT = 2;
    public static int CUSTOMER_CHARGE = 3;
    public static int CUSTOMER_PAYMENT = 4;

    private String id;

    @JsonProperty(value = "data")
    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty(value = "codicecausale")
    private String reasonCode;

    @JsonProperty(value = "nomecausale")
    private String reasonDescription;

    @JsonProperty(value = "segnocausale")
    private String reasonSign;

    @JsonProperty(value = "codicecontabile")
    private String accountingCode;

    @JsonProperty(value = "descrizione")
    private String description;

    @JsonProperty(value = "importo")
    private BigDecimal amount;

    private int type;

    @Override
    public AccountingGasEntryDTO fromModel(AccountingGasEntry model) {
        id = model.getId();
        date = model.getDate();
        description = model.getDescription();
        amount = model.getAmount();

        AccountingEntryReason reason = model.getReason();
        reasonCode = reason.getReasonCode();
        reasonDescription = reason.getDescription() + " (" + reason.getSign() + ")";
        reasonSign = reason.getSign();
        accountingCode = reason.getAccountingCode();

        if (reason.getSign().equals("+"))
            type = SUPPLIER_INVOICE;
        else
            type = SUPPLIER_PAYMENT;

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

    public AccountingGasEntryDTO fromOrderInvoice(OrderAccountingInfoDTO order) {
        date = order.getInvoiceDate();
        description = order.getDescription() + " - Fattura N. " + order.getInvoiceNumber().trim();
        reasonDescription = "Fattura ordine";
        accountingCode = order.getAccountingCode();
        amount = order.getInvoiceAmount();
        type = SUPPLIER_INVOICE;
        return this;
    }

    public AccountingGasEntryDTO fromOrderPayment(OrderAccountingInfoDTO order) {
        date = order.getPaymentDate();
        description = order.getDescription() + " - Fattura N. " + order.getInvoiceNumber().trim();
        reasonDescription = "Pagamento ordine";
        accountingCode = order.getAccountingCode();
        amount = order.getInvoiceAmount().negate();
        type = SUPPLIER_PAYMENT;
        return this;
    }

    public AccountingGasEntryDTO fromOrderTotal(OrderTotal total) {
        id = total.getOrderId();
        date = total.getDeliveryDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        description = "Totale addebiti " + total.getDescription();
        amount = total.getTotal();
        accountingCode = "C_XXX";
        type = CUSTOMER_CHARGE;
        return this;
    }
}
