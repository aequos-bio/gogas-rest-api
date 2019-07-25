package eu.aequos.gogas.excel.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemExport {
    private String productId;
    private String userId;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
}
