package eu.aequos.gogas.excel.products;

import eu.aequos.gogas.service.pricelist.ExternalPriceListCategory;
import lombok.Data;

@Data
public class ExcelPriceListCategory implements ExternalPriceListCategory {

    private final String description;

    @Override
    public String getPriceListColor() {
        return "FFFFFF";
    }

    @Override
    public int getPriceListOrder() {
        return 1;
    }
}
