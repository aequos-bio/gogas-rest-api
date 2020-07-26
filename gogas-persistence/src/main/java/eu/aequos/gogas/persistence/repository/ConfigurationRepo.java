package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Configuration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ConfigurationRepo extends CrudRepository<Configuration, String> {
  @Override
  List<Configuration> findAll();

  @Query("SELECT c.value FROM Configuration c WHERE c.key = ?1")
  Optional<String> findValueByKey(String key);

  List<Configuration> findByVisibleOrderByKey(boolean visible);

  List<Configuration> findByKeyLike(String key);

  @Transactional
  @Modifying
  @Query("UPDATE Configuration c SET c.value = ?2 WHERE c.key = ?1")
  int updateConfiguration(String key, String value);
}
