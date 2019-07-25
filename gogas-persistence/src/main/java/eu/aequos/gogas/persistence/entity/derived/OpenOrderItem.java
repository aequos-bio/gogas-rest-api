package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface OpenOrderItem {

    String getId();
    String getProduct();
    BigDecimal getOrderedQuantity();
    String getUm();
    BigDecimal getDeliveredQuantity();
    BigDecimal getPrice();
    boolean isCancelled();
}
