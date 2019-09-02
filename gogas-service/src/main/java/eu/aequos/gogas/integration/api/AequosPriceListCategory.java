package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AequosPriceListCategory {

    @JsonProperty("id")
    String id;

    @JsonProperty("descrizione")
    String description;

    @JsonProperty("colore_listino")
    String priceListColor;

    @JsonProperty("ordine_listino")
    int priceListOrder;
}
