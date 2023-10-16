package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@WithTenant("integration-test")
public class MockConfigurationData {

    @Autowired
    private ConfigurationRepo configurationRepo;

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
}
