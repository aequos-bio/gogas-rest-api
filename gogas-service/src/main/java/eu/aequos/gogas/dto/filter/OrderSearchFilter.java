package eu.aequos.gogas.dto.filter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@ApiModel("OrderSearchFilter")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OrderSearchFilter {

    @ApiModelProperty("Order Type id")
    private String orderType;

    @ApiModelProperty("Due date starting from")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate dueDateFrom;

    @ApiModelProperty("Due date until")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate dueDateTo;

    @ApiModelProperty("Delivery date starting from")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate deliveryDateFrom;

    @ApiModelProperty("Delivery date until")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    public LocalDate deliveryDateTo;

    @ApiModelProperty("Special filter to retrieve order in delivery")
    public Boolean inDelivery;

    @ApiModelProperty("List of status cods to search")
    public List<Integer> status;

    @ApiModelProperty("Special filter to retrieve order that are paid")
    public Boolean paid;
}
