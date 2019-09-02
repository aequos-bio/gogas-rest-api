package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Value
public class OrderSynchroInfoDTO {

    private Integer aequosOrderId;

    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private Date lastSynchro;
}
