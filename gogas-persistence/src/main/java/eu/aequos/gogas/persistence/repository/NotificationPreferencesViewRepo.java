package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NotificationPreferencesViewRepo extends CrudRepository<NotificationPreferencesView, NotificationPreferencesView.NotificationPreferencesViewPK> {

    List<NotificationPreferencesView> findByOrderTypeId(String orderTypeId);
}
