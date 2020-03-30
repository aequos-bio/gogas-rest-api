package eu.aequos.gogas.persistence.entity.derived;

import java.math.BigDecimal;

public interface UserCoreInfo {
    String getId();
    String getFirstName();
    String getLastName();
    boolean isEnabled();
    BigDecimal getBalance();
}
