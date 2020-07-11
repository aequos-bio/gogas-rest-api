package eu.aequos.gogas.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("Association between order type and accounting code")
@Data
public class OrderTypeAccountingDTO {
    @ApiModelProperty("order type id")
    private final String id;

    @ApiModelProperty("order type name")
    private final String description;

    @ApiModelProperty("accounting code")
    private final String accountingCode;
}
