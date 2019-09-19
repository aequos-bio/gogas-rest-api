package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.UserBalanceEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface UserBalanceEntryRepo extends CrudRepository<UserBalanceEntry, String>, JpaSpecificationExecutor<UserBalanceEntry> {
}
