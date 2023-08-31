package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface UserOrderSummaryExtraction {
    String getUserId();
    String getFriendReferralId();
    int getItemsCount();
    int getAccountedItemsCount();
    BigDecimal getTotalAmount();
    boolean isAggregated();
}
