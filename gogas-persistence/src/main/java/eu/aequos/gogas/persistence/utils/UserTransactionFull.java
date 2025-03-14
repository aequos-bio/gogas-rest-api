package eu.aequos.gogas.persistence.utils;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class UserTransactionFull {
  @Id
  private String id;
  private String userId;
  private LocalDate date;
  private String description;
  private BigDecimal amount;

  private String reason;
  private String sign;
  private boolean recorded;
  private String friend;
  private String type = "O"; // O - ordine, M - movimento contabile

  public UserTransactionFull(String id, String userId, LocalDate date, String description, BigDecimal amount, String reason, String sign, boolean recorded) {
    this.id = id;
    this.userId = id;
    this.date = date;
    this.description = description;
    this.amount = amount;
    this.reason = reason;
    this.sign = sign;
    this.recorded = recorded;
  }

  public UserTransactionFull() {
  }

  public UserTransactionFull(AccountingEntry transaction, AccountingEntryReason reason) {
      this.setId(transaction.getId());
      this.setUserId(transaction.getUser().getId());
      this.setDate(transaction.getDate());
      this.setDescription(transaction.getDescription());
      this.setAmount(transaction.getAmount());

      this.setReason(reason.getDescription());
      this.setSign(reason.getSign());
      this.recorded = true;
      this.type = "M";
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
