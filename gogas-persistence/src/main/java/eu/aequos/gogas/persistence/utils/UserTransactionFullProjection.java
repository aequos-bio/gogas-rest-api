package eu.aequos.gogas.persistence.utils;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface UserTransactionFullProjection {
  String getId();
  String getUserId();
  LocalDate getDate();
  String getDescription();
  BigDecimal getAmount();
  String getReason();
  String getSign();
  boolean isRecorded();
}