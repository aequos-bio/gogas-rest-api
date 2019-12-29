package eu.aequos.gogas.excel.products;

import lombok.Data;

import java.util.List;

@Data
public class ExtractProductsResponse {
    private List<ExcelPriceListItem> priceListItems;
    private ExtractProductError error;
}
