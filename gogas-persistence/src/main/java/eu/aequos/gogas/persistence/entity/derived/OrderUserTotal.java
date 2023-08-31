package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface OrderUserTotal {
    String getUserId();
    String getFriendReferralId();
    BigDecimal getAmount();
    BigDecimal getShippingCost();
}
