package eu.aequos.gogas.excel.products;

import eu.aequos.gogas.service.pricelist.ExternalPriceList;
import eu.aequos.gogas.service.pricelist.ExternalPriceListCategory;
import eu.aequos.gogas.service.pricelist.ExternalPriceListItem;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Data
public class ExcelPriceList implements ExternalPriceList {

    private Map<String, List<ExternalPriceListItem>> products;
    private Map<String, List<ExternalPriceListCategory>> categories;

    public ExcelPriceList(List<ExcelPriceListItem> priceListItems) {
        products = priceListItems.stream()
                .map(item -> (ExternalPriceListItem) item)
                .collect(Collectors.groupingBy(ExternalPriceListItem::getSupplierExternalId));

        categories = priceListItems.stream()
                .map(ExcelPriceListItem::getCategory)
                .distinct()
                .collect(Collectors.toMap(identity(), c -> Collections.singletonList(new ExcelPriceListCategory(c))));
    }
}
