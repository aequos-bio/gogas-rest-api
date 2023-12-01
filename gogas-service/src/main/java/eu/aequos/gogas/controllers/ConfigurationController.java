package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.service.ConfigurationService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Api("Global configuration")
@IsAdmin
@RestController
@RequestMapping("api/configuration")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;

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

    @PostMapping(value = "logo")
    public BasicResponseDTO uploadLogo(@RequestParam("file") MultipartFile attachment) throws IOException, GoGasException {
        byte[] logoFileContent = IOUtils.toByteArray(attachment.getInputStream());
        configurationService.storeLogo(logoFileContent, attachment.getContentType());
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "logo")
    public void downloadLogo(HttpServletResponse response) throws IOException, GoGasException {
        AttachmentDTO invoiceAttachment = configurationService.readLogo();
        invoiceAttachment.writeToHttpResponse(response);
    }

    @DeleteMapping(value = "logo")
    public BasicResponseDTO removeLogo() throws GoGasException {
        configurationService.removeLogo();
        return new BasicResponseDTO("OK");
    }
}
