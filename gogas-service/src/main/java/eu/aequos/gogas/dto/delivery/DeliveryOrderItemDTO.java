package eu.aequos.gogas.dto.delivery;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryOrderItemDTO {
    private String userId;
    private BigDecimal requestedQty;
    private BigDecimal deliveredQty;
    private boolean changed;
}