package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class OrderSynchroInfoDTO {

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private final Date lastSynchro;

    private int updatedProducts;
    private Integer aequosOrderId;

    public OrderSynchroInfoDTO withAequosOrderId(Integer aequosOrderId) {
        this.aequosOrderId = aequosOrderId;
        return this;
    }

    public OrderSynchroInfoDTO withUpdatedProducts(int updatedProducts) {
        this.updatedProducts = updatedProducts;
        return this;
    }
}
