package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
public class OrderByUserDTO {

    @JsonProperty("IdUtente")
    String userId;

    @JsonProperty("NomeUtente")
    String userFullName;

    @JsonProperty("ArticoliOrdinati")
    int orderedItemsCount;

    @JsonProperty("TotaleNetto")
    BigDecimal netAmount;

    @JsonProperty("CostoTrasporto")
    BigDecimal shippingCost;

    @JsonProperty("InRosso")
    boolean negativeBalance;

    /** derived values **/

    @JsonProperty("TotaleOrdine")
    public BigDecimal getTotalAmount() {
        return notNull(netAmount).add(notNull(shippingCost));
    }

    private BigDecimal notNull(BigDecimal number) {
        return Optional.ofNullable(number).orElse(BigDecimal.ZERO);
    }
}
