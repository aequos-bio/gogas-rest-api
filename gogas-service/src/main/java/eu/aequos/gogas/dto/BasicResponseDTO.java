package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class BasicResponseDTO {
    private Object data;

    public BasicResponseDTO() {
    }

    public BasicResponseDTO(Object data) {
        this.data = data;
    }
}
