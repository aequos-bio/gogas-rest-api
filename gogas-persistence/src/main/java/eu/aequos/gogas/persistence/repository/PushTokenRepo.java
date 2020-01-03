package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.PushToken;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface PushTokenRepo extends CrudRepository<PushToken, String> {

    @Query("SELECT p.token PushToken p WHERE p.userId IN ?1")
    List<String> findTokensByUserIdIn(Set<String> userIds);
}
