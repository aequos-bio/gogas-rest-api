package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface OrderItemQtyOnly {
    String getId();
    String getUser();
    BigDecimal getDeliveredQuantity();
}
