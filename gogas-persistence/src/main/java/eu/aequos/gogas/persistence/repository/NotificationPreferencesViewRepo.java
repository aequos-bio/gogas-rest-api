package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.NotificationPreferencesView;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface NotificationPreferencesViewRepo extends CrudRepository<NotificationPreferencesView, NotificationPreferencesView.NotificationPreferencesViewPK> {

    List<NotificationPreferencesView> findByOrderTypeId(String orderTypeId);

    List<NotificationPreferencesView> findByOrderTypeIdAndUserIdIn(String orderTypeId, Set<String> userIds);
}
