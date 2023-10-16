package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.NotificationPreferences;
import org.springframework.data.repository.CrudRepository;

public interface NotificationPreferencesRepo extends CrudRepository<NotificationPreferences, String> {}
