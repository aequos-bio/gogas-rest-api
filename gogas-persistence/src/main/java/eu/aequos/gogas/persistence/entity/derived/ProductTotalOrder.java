package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface ProductTotalOrder {
    String getProduct();
    BigDecimal getTotalQuantity();
    BigDecimal getUserCount();
}
