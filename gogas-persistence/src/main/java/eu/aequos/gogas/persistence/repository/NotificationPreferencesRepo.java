package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface NotificationPreferencesRepo extends CrudRepository<NotificationPreferences, String> {

    @Modifying
    @Query("DELETE FROM NotificationPreferences p WHERE p.userId = ?1")
    int deleteByUserId(String userId);
}
