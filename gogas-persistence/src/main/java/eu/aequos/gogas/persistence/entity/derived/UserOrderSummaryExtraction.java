package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface UserOrderSummaryExtraction {

    String getUserId();
    int getItemsCount();
    int getFriendItemsCount();
    int getFriendItemsAccounted();
    BigDecimal getTotalAmount();
    BigDecimal getShippingCost();
}
