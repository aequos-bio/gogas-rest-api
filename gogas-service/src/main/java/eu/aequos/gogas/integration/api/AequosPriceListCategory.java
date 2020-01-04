package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.service.pricelist.ExternalPriceListCategory;
import lombok.Data;

@Data
public class AequosPriceListCategory implements ExternalPriceListCategory {

    @JsonProperty("id")
    String id;

    @JsonProperty("descrizione")
    String description;

    @JsonProperty("colore_listino")
    String priceListColor;

    @JsonProperty("ordine_listino")
    int priceListOrder;
}
