package eu.aequos.gogas.persistence.utils;

import eu.aequos.gogas.persistence.entity.User;

import java.math.BigDecimal;

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
