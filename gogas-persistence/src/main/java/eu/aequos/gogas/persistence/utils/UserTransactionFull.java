package eu.aequos.gogas.persistence.utils;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UserTransactionFull implements UserTransactionFullProjection {
  private String id;
  private String userId;
  private LocalDate date;
  private String description;
  private BigDecimal amount;

  private String reason;
  private String sign;
  private boolean recorded;
  private String friend;

  public UserTransactionFull(AccountingEntry transaction, AccountingEntryReason reason) {
      this.setId(transaction.getId());
      this.setUserId(transaction.getUser().getId());
      this.setDate(transaction.getDate());
      this.setDescription(transaction.getDescription());
      this.setAmount(transaction.getAmount());

      this.setReason(reason.getDescription());
      this.setSign(reason.getSign());
      this.recorded = true;
  }

  public String getId() {
      return id;
  }

  public void setId(String id) {
      this.id = id;
  }

  public String getUserId() {
      return userId;
  }

  public void setUserId(String userId) {
      this.userId = userId;
  }

  public LocalDate getDate() {
      return date;
  }

  public void setDate(LocalDate date) {
      this.date = date;
  }

  public String getDescription() {
      return description;
  }

  public void setDescription(String description) {
      this.description = description;
  }

  public BigDecimal getAmount() {
      return amount;
  }

  public void setAmount(BigDecimal amount) {
      this.amount = amount;
  }

  public String getReason() {
      return reason;
  }

  public void setReason(String reason) {
      this.reason = reason;
  }

  public String getSign() {
      return sign;
  }

  public void setSign(String sign) {
      this.sign = sign;
  }

  public boolean isRecorded() {
      return recorded;
  }

  public void setRecorded(boolean recorded) {
      this.recorded = recorded;
  }

  public String getFriend() {
      return friend;
  }

  public void setFriend(String friend) {
      this.friend = friend;
  }
}
