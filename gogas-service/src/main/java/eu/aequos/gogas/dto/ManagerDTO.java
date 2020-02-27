package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.OrderManager;
import lombok.Data;

@Data
public class ManagerDTO {
  private String id;
  private String userId;
  private String userName;
  private String orderTypeId;
  private String orderTypeName;

  public ManagerDTO() {}

  public ManagerDTO(String id, String userId, String userName, String userTypeId, String userTypeName) {
    this.id = id;
    this.userId = userId;
    this.userName = userName;
    this.orderTypeId = userTypeId;
    this.orderTypeName = getOrderTypeName();
  }

  public static ManagerDTO fromOrderManager(OrderManager orderManager) {
    ManagerDTO m = new ManagerDTO();
    m.setId(orderManager.getId());
    m.setUserId(orderManager.getUser());
    m.setOrderTypeId(orderManager.getOrderType());
    return m;
  }
}
