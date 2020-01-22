package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreationItem {

    @JsonProperty("codice")
    private String id;

    @JsonProperty("num_collo")
    private BigDecimal boxesCount;
}
