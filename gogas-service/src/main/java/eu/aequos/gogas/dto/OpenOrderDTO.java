package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OpenOrderSummary;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenOrderDTO {

    private String id;

    @JsonProperty("idtipoordine")
    private String orderTypeId;

    @JsonProperty("tipoordine")
    private String orderTypeName;

    @JsonProperty("datachiusura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private Date dueDate;

    @JsonProperty("orachiusura")
    private int dueHour;

    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private Date deliveryDate;

    private boolean external;

    @JsonProperty("externallink")
    private String externalLink;

    private List<OpenOrderSummaryDTO> userOrders;

    public OpenOrderDTO fromModel(Order order, List<OpenOrderSummary> singleOrders) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.dueDate = order.getDueDate();
        this.dueHour = order.getDueHour();
        this.deliveryDate = order.getDeliveryDate();
        this.external = order.getOrderType().isExternal();
        this.externalLink = order.getExternaLlink();

        this.userOrders = singleOrders.stream()
                .map(o -> new OpenOrderSummaryDTO().fromModel(o))
                .collect(Collectors.toList());

        return this;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class OpenOrderSummaryDTO {
        String userId;
        BigDecimal totalAmount;
        int itemsCount;

        public OpenOrderSummaryDTO fromModel(OpenOrderSummary openOrderSummary) {
            this.userId = openOrderSummary.getUserId();
            this.totalAmount = openOrderSummary.getTotalAmount();
            this.itemsCount = openOrderSummary.getItemsCount();

            return this;
        }
    }
}
