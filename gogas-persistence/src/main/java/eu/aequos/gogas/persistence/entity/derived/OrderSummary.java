package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface OrderSummary {

    String getOrderId();
    BigDecimal getTotalAmount();
}
