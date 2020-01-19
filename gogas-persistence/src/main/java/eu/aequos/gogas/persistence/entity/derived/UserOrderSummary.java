package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface UserOrderSummary extends OrderSummary {

    int getItemsCount();
    int getFriendCount();
    int getfriendAccounted();
    BigDecimal getShippingCost();
}
