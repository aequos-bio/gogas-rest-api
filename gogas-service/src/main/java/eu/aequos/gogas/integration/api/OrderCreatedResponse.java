package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderCreatedResponse extends AequosResponse {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("total_items")
    private int totalItems;
}
