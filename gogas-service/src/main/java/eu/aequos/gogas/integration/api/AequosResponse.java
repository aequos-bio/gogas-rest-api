package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AequosResponse {
    private boolean error;

    @JsonProperty("error_message")
    private String errorMessage;
}