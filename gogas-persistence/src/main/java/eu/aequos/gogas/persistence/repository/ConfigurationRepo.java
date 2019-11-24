package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Configuration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepo extends CrudRepository<Configuration, String> {
  @Override
  List<Configuration> findAll();

  Optional<Configuration> findByKey(String key);
}
