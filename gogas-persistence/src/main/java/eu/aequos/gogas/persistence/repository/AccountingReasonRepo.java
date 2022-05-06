package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountingReasonRepo extends CrudRepository<AccountingEntryReason, String> {
    List<AccountingEntryReason> findAllByOrderByDescription();
    AccountingEntryReason findByReasonCode(String reasonCode);
}
