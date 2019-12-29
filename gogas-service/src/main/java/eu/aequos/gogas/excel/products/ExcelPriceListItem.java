package eu.aequos.gogas.excel.products;

import eu.aequos.gogas.service.pricelist.ExternalPriceListItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExcelPriceListItem implements ExternalPriceListItem {
    private String externalId;
    private String name;
    private String supplierExternalId;
    private String supplierName;
    private String supplierProvince;
    private String category;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal boxWeight;
    private String notes;
    private String frequency;
    private boolean wholeBoxesOnly;
    private BigDecimal multiple;
}
