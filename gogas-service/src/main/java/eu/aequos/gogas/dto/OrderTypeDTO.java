package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.OrderType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Optional;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

@ApiModel("OrderType")
@Data
public class OrderTypeDTO implements ConvertibleDTO<OrderType> {

    @ApiModelProperty(accessMode = READ_ONLY)
    private String id;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty("descrizione")
    private String description;

    @ApiModelProperty(
        required = true,
        value = "If enabled, friends order items are merged into reference user order items when closing the order. " +
                "If disabled, friends order items are kept separately when closing the order."
    )
    @JsonProperty("riepilogo")
    private boolean summaryRequired;

    @ApiModelProperty(
        required = true,
        value = "If enabled, order cost is accounted to users by multiplying each product quantity by unit price. " +
                "If disabled, order cost is accounted manually by order manager."
    )
    @JsonProperty("totalecalcolato")
    private boolean computedAmount;

    @ApiModelProperty(
        required = true,
        value = "If enabled, advance cost is shown when user fills his order. " +
                "If disabled, advance cost is NOT shown."
    )
    @JsonProperty("preventivo")
    private boolean showAdvance;

    @ApiModelProperty(
        required = true,
        value = "If enabled, box completion status is shown when user fills his order. " +
                "If disabled, box completion status is NOT shown."
    )
    @JsonProperty("completamentocolli")
    private boolean showBoxCompletion;

    @ApiModelProperty("not implemented yet")
    @JsonProperty("turni")
    private boolean hasTurns;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty("idordineaequos")
    private Integer aequosOrderId;

    @ApiModelProperty(accessMode = READ_ONLY, value = "true if at least one order has been opened with this order type")
    @JsonProperty("utilizzata")
    private boolean used;

    @ApiModelProperty(
        required = true,
        value = "if enabled, all users (even the one with no ordered items) are shown in the order excel export"
    )
    private boolean excelAllUsers;

    @ApiModelProperty(
        required = true,
        value = "if enabled, all products (even the one not ordered) are shown in the order excel export"
    )
    private boolean excelAllProducts;

    @ApiModelProperty(
        required = true,
        value = "if enabled, the order is not managed inside GoGas but is a link opened in an external window"
    )
    private boolean external;

    @ApiModelProperty(
        value = "if the order is external, this is the URL of the link to be opened"
    )
    private String externalLink;

    @ApiModelProperty(
        accessMode = READ_ONLY,
        value = "true if the invoice of the order is produced directly by Aequos (sold by Aequos)"
    )
    private boolean billedByAequos;

    private String accountingCode;

    public OrderTypeDTO fromModel(OrderType orderType, boolean used) {
        this.used = used;
        return fromModel(orderType);
    }

    @Override
    public OrderTypeDTO fromModel(OrderType orderType) {
        this.id = orderType.getId();
        this.description = orderType.getDescription();
        this.summaryRequired = orderType.isSummaryRequired();
        this.computedAmount = orderType.isComputedAmount();
        this.showAdvance = orderType.isShowAdvance();
        this.showBoxCompletion = orderType.isShowBoxCompletion();
        this.hasTurns = orderType.isHasTurns();
        this.aequosOrderId = orderType.getAequosOrderId();
        this.external = orderType.isExternal();
        this.externalLink = orderType.getExternallink();
        this.excelAllUsers = orderType.isExcelAllUsers();
        this.excelAllProducts = orderType.isExcelAllProducts();
        this.billedByAequos = orderType.isBilledByAequos();
        this.accountingCode = orderType.getAccountingCode();

        return this;
    }

    @Override
    public OrderType toModel(Optional<OrderType> existingModel) {
        OrderType model = existingModel.orElse(new OrderType());
        model.setDescription(this.description);
        model.setComputedAmount(this.computedAmount);
        model.setSummaryRequired(this.summaryRequired);
        model.setShowAdvance(this.showAdvance);
        model.setShowBoxCompletion(this.showBoxCompletion);
        model.setHasTurns(this.hasTurns);
        model.setExcelAllProducts(this.excelAllProducts);
        model.setExcelAllUsers(this.excelAllUsers);
        model.setExternal(this.external);
        model.setExternallink(this.externalLink);
        model.setAccountingCode(this.accountingCode);

        return model;
    }
}
