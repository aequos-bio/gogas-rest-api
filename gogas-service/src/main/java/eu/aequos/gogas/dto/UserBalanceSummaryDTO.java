package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public final class UserBalanceSummaryDTO {

    @JsonProperty(value = "totale")
    private final BigDecimal balance;

    @JsonProperty(value = "movimenti")
    private final List<UserBalanceEntryDTO> entries;
}
