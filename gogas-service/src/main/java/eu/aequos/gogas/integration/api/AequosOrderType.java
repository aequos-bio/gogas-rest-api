package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AequosOrderType {

    @JsonProperty("id")
    private int id;

    @JsonProperty("descrizione")
    private String description;

    @JsonProperty("fatturato_da_aequos")
    private int billedByAequos;

    public boolean isBilledByAequos() {
        return billedByAequos > 0;
    }
}
