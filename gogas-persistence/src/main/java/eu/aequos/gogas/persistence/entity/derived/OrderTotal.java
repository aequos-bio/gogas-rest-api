package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;
import java.util.Date;

public interface OrderTotal {
  String getOrderId();
  String getDescription();
  Date getDeliveryDate();
  BigDecimal getTotal();
}
