package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface AccountingGasRepo extends CrudRepository<AccountingGasEntry, String>, JpaSpecificationExecutor<AccountingGasEntry> {
}
