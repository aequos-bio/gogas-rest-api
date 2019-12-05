package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface FriendTotalOrder {
    String getProduct();
    BigDecimal getTotalQuantity();
    BigDecimal getUserCount();
    boolean isAccounted();
}
