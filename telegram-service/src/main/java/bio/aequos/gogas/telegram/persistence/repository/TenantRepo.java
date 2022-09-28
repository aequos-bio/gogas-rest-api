package bio.aequos.gogas.telegram.persistence.repository;

import bio.aequos.gogas.telegram.persistence.model.TenantEntity;
import org.springframework.data.repository.CrudRepository;

public interface TenantRepo extends CrudRepository<TenantEntity, String> {}
