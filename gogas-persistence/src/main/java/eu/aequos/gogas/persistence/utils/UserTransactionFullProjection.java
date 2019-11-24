package eu.aequos.gogas.persistence.utils;

import java.math.BigDecimal;
import java.util.Date;

public interface UserTransactionFullProjection {
  String getId();
  String getUserId();
  Date getDate();
  String getDescription();
  BigDecimal getAmount();
  String getReason();
  String getSign();
  boolean isRecorded();
}