package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.Tenant;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TenantRepo extends CrudRepository<Tenant, Long> {

    @Override
    List<Tenant> findAll();
}
