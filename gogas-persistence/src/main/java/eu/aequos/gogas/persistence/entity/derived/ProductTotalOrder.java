package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface ProductTotalOrder {
    String getProduct();
    String getProductExternalId();
    BigDecimal getTotalQuantity();
    BigDecimal getUserCount();
    boolean isCancelled();
}
