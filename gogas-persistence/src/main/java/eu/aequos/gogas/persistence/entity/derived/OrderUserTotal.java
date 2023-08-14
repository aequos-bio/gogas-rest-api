package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface OrderUserTotal {
    String getUserId();
    String getFriendReferralId();
    BigDecimal getAmount();
    BigDecimal getShippingCost();

    default boolean isUserOrFriend(String userId) {
        return userId.equals(getFriendReferralId()) || userId.equals(getUserId());
    }

    default BigDecimal getTotalAmount() {
        BigDecimal shippingCost = getShippingCost();
        BigDecimal amount = getAmount();

        if (shippingCost == null) {
            return amount;
        }

        return amount.add(shippingCost);
    }
}
