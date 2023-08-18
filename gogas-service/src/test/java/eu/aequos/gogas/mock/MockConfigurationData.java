package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MockConfigurationData implements MockDataLifeCycle {

    private final ConfigurationService configurationService;

    @WithTenant("integration-test")
    public void prepareExistingLogo(InputStream logoInputStream) throws IOException {
        ByteArrayOutputStream logoOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(logoInputStream, logoOutputStream);
        byte[] logoFileContent = logoOutputStream.toByteArray();
        configurationService.storeLogo(logoFileContent, "image/png");
    }

    @Override
    public void init() {
        configurationService.resetProperties();
    }

    @Override
    public void destroy() {
        configurationService.resetProperties();
    }
}
