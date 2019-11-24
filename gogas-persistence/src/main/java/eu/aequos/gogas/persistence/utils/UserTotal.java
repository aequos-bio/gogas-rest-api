package eu.aequos.gogas.persistence.utils;

import java.math.BigDecimal;

import eu.aequos.gogas.persistence.entity.User;

public class UserTotal {
  private User user;
  private BigDecimal total;

  public UserTotal(User user, BigDecimal total) {
      this.user = user;
      this.setTotal(total);
  }


  public User getUser() {
      return user;
  }

  public BigDecimal getTotal() {
      return total;
  }

  public void setTotal(BigDecimal total) {
      this.total = total;
  }
}
