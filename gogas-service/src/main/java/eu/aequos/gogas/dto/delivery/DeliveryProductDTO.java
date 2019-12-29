package eu.aequos.gogas.dto.delivery;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DeliveryProductDTO {
    private String productName;
    private String productId;
    private String unitOfMeasure;
    private BigDecimal boxWeight;
    private BigDecimal price;
    private BigDecimal orderedBoxes;

    private List<DeliveryOrderItemDTO> orderItems;
}
