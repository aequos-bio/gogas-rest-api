package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface AccountingRepo extends CrudRepository<AccountingEntry, String>, JpaSpecificationExecutor<AccountingEntry> {

    @Procedure(name = "User.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);
}
