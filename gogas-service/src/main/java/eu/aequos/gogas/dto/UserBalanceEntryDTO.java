package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class UserBalanceEntryDTO {

    @JsonProperty(value = "idRiga")
    private String id;

    @JsonProperty(value = "descrizione")
    private String description;

    @JsonProperty(value = "data")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate date;

    @JsonProperty(value = "importo")
    private BigDecimal amount;

    private String orderId;
    private String orderType;
    private boolean external;
    private String externalLink;


    public UserBalanceEntryDTO fromModel(AccountingEntry model, Map<String, Order> orders, String userId) {
        id = model.getId();
        date = model.getDate();
        description = buildDescription(model, userId);
        amount = getSignedAmount(model.getAmount(), model.getReason().getSign());

        extractRelatedOrder(model.getOrderId(), orders)
                .ifPresent(relatedOrder -> {
                    orderId = relatedOrder.getId();
                    orderType = relatedOrder.getOrderType().getId();
                    externalLink = relatedOrder.getExternaLlink();
                    external = externalLink != null;
                });

        return this;
    }

    private String buildDescription(AccountingEntry model, String userId) {
        String baseDescription = model.getReason().getDescription() + " - " + model.getDescription();

        if (userId.equalsIgnoreCase(model.getUser().getId())) {
            return baseDescription;
        }

        return baseDescription + " (" + model.getUser().getFirstName() + " " + model.getUser().getLastName() + ")";
    }

    private Optional<Order> extractRelatedOrder(String orderId, Map<String, Order> orders) {
        if (orderId == null || !orders.containsKey(orderId)) {
            return Optional.empty();
        }

        return Optional.of(orders.get(orderId));
    }

    private BigDecimal getSignedAmount(BigDecimal unsignedAmount, String sign) {
        return AccountingEntryReason.Sign.fromSymbol(sign)
                .getSignedAmount(unsignedAmount);
    }
}
