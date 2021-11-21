package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.derived.OrderSummary;
import eu.aequos.gogas.persistence.entity.derived.UserOrderSummary;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class UserOrderDetailsDTO {

    private String id;

    @JsonProperty("idtipoordine")
    private String orderTypeId;

    @JsonProperty("tipoordine")
    private String orderTypeName;

    @JsonProperty("datachiusura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate dueDate;

    @JsonProperty("dataconsegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    @JsonProperty("aperto")
    private boolean open;

    private boolean editable;

    @JsonProperty("mostraPreventivo")
    private boolean showAdvance;

    @JsonProperty("completamentoColli")
    private boolean showBoxCompletion;

    private BigDecimal totalAmount;

    public UserOrderDetailsDTO fromModel(Order order) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderType().getId();
        this.orderTypeName = order.getOrderType().getDescription();
        this.dueDate = order.getDueDate();
        this.deliveryDate = order.getDeliveryDate();
        this.open = order.getStatus().isOpen();
        this.editable = order.isEditable();
        this.showAdvance = order.getOrderType().isShowAdvance();
        this.showBoxCompletion = order.getOrderType().isShowBoxCompletion();

        return this;
    }

    public UserOrderDetailsDTO withTotalAmount(Optional<UserOrderSummary> orderTotalAmount) {
        this.totalAmount = orderTotalAmount.map(OrderSummary::getTotalAmount)
                .orElse(BigDecimal.ZERO);

        return this;
    }
}
