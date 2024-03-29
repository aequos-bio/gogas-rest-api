package eu.aequos.gogas.service.pricelist;

import java.util.List;
import java.util.Map;

public interface ExternalPriceList {
    Map<String, List<ExternalPriceListItem>> getProducts();
    List<ExternalPriceListCategory> getCategories();
}
