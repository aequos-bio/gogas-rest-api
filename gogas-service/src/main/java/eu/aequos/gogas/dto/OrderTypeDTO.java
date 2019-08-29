package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.Product;
import lombok.Data;

import javax.persistence.Column;
import java.util.Date;
import java.util.Optional;

@Data
public class OrderTypeDTO implements ConvertibleDTO<OrderType> {

    private String id;

    @JsonProperty("descrizione")
    private String description;

    @JsonProperty("riepilogo")
    private boolean summaryRequired;

    @JsonProperty("totalecalcolato")
    private boolean computedAmount;

    @JsonProperty("preventivo")
    private boolean showAdvance;

    @JsonProperty("completamentocolli")
    private boolean showBoxCompletion;

    @JsonProperty("turni")
    private boolean hasTurns;

    @JsonProperty("idordineaequos")
    private Integer aequosOrderId;

    @JsonProperty("utilizzata")
    private boolean used;

    private boolean excelAllUsers;

    private boolean excelAllProducts;

    private boolean external;

    private String externalLink;

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

        return model;
    }
}
