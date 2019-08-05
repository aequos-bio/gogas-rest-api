package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AccountingRepo extends CrudRepository<AccountingEntry, String>, JpaSpecificationExecutor<AccountingEntry> {

    @Procedure(name = "User.balance")
    BigDecimal getBalance(@Param("idUtente") String userId);

    List<AccountingEntry> findByOrderId(String orderId);

    long countByOrderId(String orderId);

    @Modifying
    @Query("UPDATE AccountingEntry a SET a.confirmed = ?2 WHERE a.orderId = ?1")
    int setConfirmedByOrderId(String orderId, boolean confirmed);
}
