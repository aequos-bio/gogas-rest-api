package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.ConfigurationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@IsAdmin
@RestController
@RequestMapping("api/configuration")
public class ConfigurationController {

    private ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public List<ConfigurationItemDTO> getVisible() {
        return configurationService.getVisibleConfigurationItems();
    }

    @PutMapping
    public BasicResponseDTO update(@RequestBody ConfigurationItemDTO configurationItem) throws GoGasException {
        boolean configurationFound = configurationService.updateConfigurationItem(configurationItem);

        if (!configurationFound)
            throw new ItemNotFoundException("configuration", configurationItem.getKey());

        return new BasicResponseDTO("OK");
    }
}
