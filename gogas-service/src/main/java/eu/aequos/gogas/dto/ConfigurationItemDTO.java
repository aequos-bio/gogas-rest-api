package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.Configuration;
import lombok.Data;

@Data
public class ConfigurationItemDTO {
    private String key;
    private String value;
    private String description;

    public ConfigurationItemDTO fromModel(Configuration configuration) {
        this.key = configuration.getKey();
        this.value = configuration.getValue();
        this.description = configuration.getDescription();

        return this;
    }
}
