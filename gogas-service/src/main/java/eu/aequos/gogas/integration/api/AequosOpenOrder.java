package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class AequosOpenOrder {

    @JsonProperty("id")
    private int id;

    @JsonProperty("descrizione")
    private String description;

    @JsonProperty("apertura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate openingDate;

    @JsonProperty("chiusura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate dueDate;

    @JsonProperty("consegna")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    private LocalDate deliveryDate;
}
