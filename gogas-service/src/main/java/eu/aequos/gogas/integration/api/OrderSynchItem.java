package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSynchItem {

    @JsonProperty("codice")
    private String id;

    @JsonProperty("qta")
    private BigDecimal quantity;

    @JsonProperty("prezzo")
    private BigDecimal price;
}
