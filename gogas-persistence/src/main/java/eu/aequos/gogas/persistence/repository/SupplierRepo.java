package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Supplier;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SupplierRepo extends CrudRepository<Supplier, String> {

    Optional<Supplier> findByExternalId(String externalId);
}
