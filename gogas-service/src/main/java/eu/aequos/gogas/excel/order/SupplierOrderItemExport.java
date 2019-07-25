package eu.aequos.gogas.excel.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierOrderItemExport {
    private String productId;
    private BigDecimal unitPrice;
    private BigDecimal boxWeight;
    private BigDecimal quantity;
}
