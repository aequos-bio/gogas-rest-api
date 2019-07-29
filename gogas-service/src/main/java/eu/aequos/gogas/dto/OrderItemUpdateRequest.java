package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemUpdateRequest {

    @JsonProperty("idUtente")
    String userId;

    @JsonProperty("um")
    String unitOfMeasure;

    @JsonProperty("qta")
    BigDecimal quantity;

    @JsonProperty("id")
    String productId;
}
