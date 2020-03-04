package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingGasEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface AccountingGasRepo extends CrudRepository<AccountingGasEntry, String>, JpaSpecificationExecutor<AccountingGasEntry> {

    List<AccountingGasEntry> findByDateBetween(LocalDate dateFrom, LocalDate dateTo);
}
