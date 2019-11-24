package eu.aequos.gogas.persistence.utils;

import java.math.BigDecimal;

public interface UserTotalProjection {
    String getUserId();
    BigDecimal getTotal();
}
