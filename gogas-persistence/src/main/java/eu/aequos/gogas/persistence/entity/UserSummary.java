package eu.aequos.gogas.persistence.entity;

import org.springframework.beans.factory.annotation.Value;

public interface UserSummary {
    String getId();
    String getFirstName();
    String getLastName();

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getCompleteName();
}
