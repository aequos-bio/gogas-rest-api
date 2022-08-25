package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Data
public class OrderItemUpdateRequest {

    @NotNull
    @JsonProperty("idUtente")
    String userId;

    @JsonProperty("um")
    String unitOfMeasure;

    @PositiveOrZero
    @JsonProperty("qta")
    BigDecimal quantity;

    @NotNull
    @JsonProperty("id")
    String productId;
}
