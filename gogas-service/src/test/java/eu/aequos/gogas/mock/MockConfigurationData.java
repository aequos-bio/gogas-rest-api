package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import eu.aequos.gogas.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@WithTenant("integration-test")
public class MockConfigurationData implements MockDataLifeCycle {

    private final ConfigurationService configurationService;
    private final ConfigurationRepo configurationRepo;

    public void removeSortUsersByPosition() {
        configurationRepo.findById("users.position")
                .ifPresent(configurationRepo::delete);
    }

    public void enableSortUsersByPosition() {
        Configuration configurationFlag = new Configuration();
        configurationFlag.setKey("users.position");
        configurationFlag.setValue("true");
        configurationRepo.save(configurationFlag);
    }

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
