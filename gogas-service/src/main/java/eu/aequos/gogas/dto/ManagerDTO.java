package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.OrderManager;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("Order manager")
@Data
public class ManagerDTO {

  @ApiModelProperty("association id")
  private String id;

  @ApiModelProperty("manager user id")
  private String userId;

  @ApiModelProperty("manager name")
  private String userName;

  @ApiModelProperty("managed order type id")
  private String orderTypeId;

  @ApiModelProperty("managed order type name")
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
