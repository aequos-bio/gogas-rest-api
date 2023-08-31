package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.UserOrderSummary;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private LocalDate dueDate;

    @JsonProperty("orachiusura")
    private int dueHour;

    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    private boolean external;

    @JsonProperty("externallink")
    private String externalLink;

    private boolean showAdvance;

    private List<OpenOrderSummaryDTO> userOrders;

    public OpenOrderDTO fromModel(Order order, List<UserOrderSummary> singleOrders, Map<String, User> userMap) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.dueDate = order.getDueDate();
        this.dueHour = order.getDueHour();
        this.deliveryDate = order.getDeliveryDate();
        this.external = order.getOrderType().isExternal();
        this.externalLink = order.getExternaLlink();
        this.showAdvance = order.getOrderType().isShowAdvance();

        this.userOrders = singleOrders.stream()
                .map(o -> new OpenOrderSummaryDTO().fromModel(o, userMap))
                .collect(Collectors.toList());

        return this;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OpenOrderSummaryDTO {
        String userId;
        String firstname;
        String lastname;
        BigDecimal totalAmount;
        int itemsCount;

        public OpenOrderSummaryDTO fromModel(UserOrderSummary userOrderSummary, Map<String, User> userMap) {
            this.userId = userOrderSummary.getUserId();
            User u = userMap.get(this.userId);
            this.firstname = u == null ? "?" : u.getFirstName();
            this.lastname = u == null ? "?" : u.getLastName();
            this.totalAmount = userOrderSummary.getTotalAmount();
            this.itemsCount = userOrderSummary.getItemsCount();

            return this;
        }
    }
}
