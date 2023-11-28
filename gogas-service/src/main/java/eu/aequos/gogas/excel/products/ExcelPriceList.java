package eu.aequos.gogas.excel.products;

import eu.aequos.gogas.service.pricelist.ExternalPriceList;
import eu.aequos.gogas.service.pricelist.ExternalPriceListCategory;
import eu.aequos.gogas.service.pricelist.ExternalPriceListItem;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ExcelPriceList implements ExternalPriceList {

    private Map<String, List<ExternalPriceListItem>> products;
    private List<ExternalPriceListCategory> categories;

    public ExcelPriceList(List<ExcelPriceListItem> priceListItems) {
        products = priceListItems.stream()
                .map(item -> (ExternalPriceListItem) item)
                .collect(Collectors.groupingBy(ExternalPriceListItem::getSupplierExternalId));

        categories = priceListItems.stream()
                .map(ExcelPriceListItem::getCategory)
                .distinct()
                .map(ExcelPriceListCategory::new)
                .collect(Collectors.toList());
    }
}
