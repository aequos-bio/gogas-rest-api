package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WeightsUpdatedResponse extends AequosResponse {

    @JsonProperty("updated_rows")
    private List<String> updatedItems;
}
