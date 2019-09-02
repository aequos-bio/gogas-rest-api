package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AequosPriceList {

    @JsonProperty("articoli")
    Map<String, List<AequosPriceListItem>> products;

    @JsonProperty("categorie")
    Map<String, List<AequosPriceListCategory>> categories;
}
