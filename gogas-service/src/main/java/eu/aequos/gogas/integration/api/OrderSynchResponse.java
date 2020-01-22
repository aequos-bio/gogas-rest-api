package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSynchResponse extends AequosResponse {

    @JsonProperty("order_rows")
    private List<OrderSynchItem> orderItems;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("order_total_amount")
    private BigDecimal orderTotalAmount;
}
