package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface UserOrderSummaryDerived extends OrderSummary {
    int getItemsCount();
    int getFriendCount();
    int getFriendAccounted();
    BigDecimal getShippingCost();
}
