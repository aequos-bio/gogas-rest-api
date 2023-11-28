package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.service.pricelist.ExternalPriceList;
import eu.aequos.gogas.service.pricelist.ExternalPriceListCategory;
import eu.aequos.gogas.service.pricelist.ExternalPriceListItem;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Setter
public class AequosPriceList implements ExternalPriceList {

    @JsonProperty("articoli")
    private Map<String, List<AequosPriceListItem>> products;

    @JsonProperty("categorie")
    private List<AequosPriceListCategory> categories;

    @Override
    public Map<String, List<ExternalPriceListItem>> getProducts() {
        //remapping due to java type incompatibility
        return products.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    @Override
    public List<ExternalPriceListCategory> getCategories() {
        //remapping due to java type incompatibility
        return new ArrayList<>(categories);
    }
}
