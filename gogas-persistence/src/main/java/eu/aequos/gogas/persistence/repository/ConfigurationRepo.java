package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Configuration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepo extends CrudRepository<Configuration, String> {
}
