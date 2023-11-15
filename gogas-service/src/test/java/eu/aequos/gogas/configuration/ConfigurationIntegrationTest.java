package eu.aequos.gogas.configuration;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.service.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConfigurationIntegrationTest extends BaseGoGasIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Test
    void givenNoExistingLogoAndAValidLogoImage_whenSendingLogo_thenFileIsStoredInTheRightFolder() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        InputStream logoInputStream = getClass().getResourceAsStream("logo.png");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/configuration/logo", logoInputStream, "logo_gastabien.png", "image/png", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        File logo = repoFolder.resolve("integration-test")
                .resolve("logo")
                .resolve("logo")
                .toFile();

        assertTrue(logo.exists());
        assertEquals(20683, logo.length());
    }

    @Test
    void givenNoExistingLogoAndAValidLogoImage_whenSendingLogo_thenLogoIsStoredAndAvailableForDownload() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        InputStream logoInputStream = getClass().getResourceAsStream("logo.png");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/configuration/logo", logoInputStream, "logo_gastabien.png", "image/png", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        byte[] contentAsByteArray = mockMvcGoGas.get("/api/configuration/logo")
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"logo.png\""))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertEquals(20683, contentAsByteArray.length);
    }

    @Test
    void givenAnExistingLogoAndAValidLogoImage_whenSendingLogo_thenLogoIsOverwrittenAndAvailableForDownload() throws Exception {
        try (InputStream logoInputStream = getClass().getResourceAsStream("logo.png")) {
            mockConfigurationData.prepareExistingLogo(logoInputStream);
        }

        mockMvcGoGas.loginAsAdmin();

        InputStream logoInputStream = getClass().getResourceAsStream("aequos.jpg");
        BasicResponseDTO updateResponse = mockMvcGoGas.postDTO("/api/configuration/logo", logoInputStream, "logo_aequos.jpg", "image/jpeg", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        byte[] contentAsByteArray = mockMvcGoGas.get("/api/configuration/logo")
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"logo.jpg\""))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertEquals(4686, contentAsByteArray.length);
    }

    @Test
    void givenAnExistingLogo_whenRemovingLogo_thenLogoIsRemoved() throws Exception {
        try (InputStream logoInputStream = getClass().getResourceAsStream("logo.png")) {
            mockConfigurationData.prepareExistingLogo(logoInputStream);
        }

        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO updateResponse = mockMvcGoGas.deleteDTO("/api/configuration/logo", BasicResponseDTO.class);
        assertEquals("OK", updateResponse.getData());

        mockMvcGoGas.get("/api/configuration/logo")
                .andExpect(status().isNotFound());

        File logo = repoFolder.resolve("integration-test")
                .resolve("logo")
                .resolve("logo")
                .toFile();

        assertFalse(logo.exists());
    }

    @Test
    void givenNoExistingLogo_whenGettingLogo_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.get("/api/configuration/logo")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoExistingLogo_whenRemovingLogo_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.delete("/api/configuration/logo")
                .andExpect(status().isNotFound());
    }

    //TODO: check errors for invalid data or invalid roles
}
