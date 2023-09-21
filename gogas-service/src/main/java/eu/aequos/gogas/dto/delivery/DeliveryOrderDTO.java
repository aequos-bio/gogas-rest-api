package eu.aequos.gogas.dto.delivery;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.aequos.gogas.dto.UserDTO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class DeliveryOrderDTO {

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;

    private String orderId;
    private String orderType;

    private List<UserDTO> users;
    private List<DeliveryProductDTO> products;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sortUsersByName;
}
