package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface ByUserOrderItem {
    String getUserId();
    int getOrderedItems();
    BigDecimal getTotalAmount();
}
