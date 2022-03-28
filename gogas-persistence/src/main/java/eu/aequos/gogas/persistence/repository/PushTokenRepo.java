package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.PushToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface PushTokenRepo extends CrudRepository<PushToken, String> {

    @Query("SELECT p.token FROM PushToken p WHERE p.userId IN ?1")
    List<String> findTokensByUserIdIn(Set<String> userIds);

    @Modifying
    @Query("DELETE FROM PushToken p WHERE p.userId = ?1 AND p.token = ?2")
    int deleteByUserIdAndToken(String userId, String token);
}
