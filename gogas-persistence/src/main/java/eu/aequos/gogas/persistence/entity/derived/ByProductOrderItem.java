package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface ByProductOrderItem {
    String getId();
    String getUser();
    BigDecimal getOrderedQuantity();
    String getUm();
    BigDecimal getDeliveredQuantity();
    boolean isCancelled();
}
