package eu.aequos.gogas.dto.delivery;

import eu.aequos.gogas.dto.UserDTO;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryOrderDTO {
    private String orderId;
    private List<UserDTO> users;
    private List<DeliveryProductDTO> products;
}
