package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.order.GoGasOrder;
import lombok.Data;

import java.time.LocalDate;

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

    @JsonProperty("mostraPreventivo")
    private boolean showAdvance;

    @JsonProperty("completamentoColli")
    private boolean showBoxCompletion;

    public UserOrderDetailsDTO fromModel(GoGasOrder order) {
        this.id = order.getId();
        this.orderTypeId = order.getOrderTypeId();
        this.orderTypeName = order.getOrderTypeDescription();
        this.dueDate = order.getModel().getDueDate();
        this.deliveryDate = order.getDeliveryDate();
        this.open = order.getStatus().isOpen();
        this.showAdvance = order.showAdvance();
        this.showBoxCompletion = order.showBoxCompletion();

        return this;
    }
}
