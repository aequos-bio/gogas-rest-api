package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderTypeSelectItemDTO extends SelectItemDTO {

    @JsonProperty("idordineaequos")
    private Integer aequosOrderId;

    private boolean external;
    private String externalLink;

    public OrderTypeSelectItemDTO(String id, String description,
                                  Integer aequosOrderId, boolean external,
                                  String externalLink) {

        super(id, description);
        this.aequosOrderId = aequosOrderId;
        this.external = external;
        this.externalLink = externalLink;
    }
}
