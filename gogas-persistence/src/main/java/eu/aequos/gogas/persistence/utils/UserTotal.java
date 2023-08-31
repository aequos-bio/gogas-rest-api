package eu.aequos.gogas.persistence.utils;

import java.math.BigDecimal;

import eu.aequos.gogas.persistence.entity.User;

public class UserTotal {
  private User user;

  public UserTotal(User user) {
      this.user = user;
  }

  public User getUser() {
      return user;
  }

  public BigDecimal getTotal() {
      return user.getBalance();
  }
}
