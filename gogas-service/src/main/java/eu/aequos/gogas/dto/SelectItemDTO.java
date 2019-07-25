package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class SelectItemDTO {
    private String id;
    private String description;

    public SelectItemDTO() {}

    public SelectItemDTO(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public static SelectItemDTO empty(String description) {
        return new SelectItemDTO("", description);
    }
}
