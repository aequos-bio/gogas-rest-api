package eu.aequos.gogas.excel.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductExport {
    private String id;
    private String name;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal boxWeight;
}
